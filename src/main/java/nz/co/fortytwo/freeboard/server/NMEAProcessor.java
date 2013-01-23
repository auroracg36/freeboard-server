/*
 * Copyright 2012,2013 Robert Huitema robert@42.co.nz
 * 
 * This file is part of FreeBoard. (http://www.42.co.nz/freeboard)
 * 
 * FreeBoard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FreeBoard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with FreeBoard. If not, see <http://www.gnu.org/licenses/>.
 */
package nz.co.fortytwo.freeboard.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.HeadingSentence;
import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.sentence.PositionSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.VHWSentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import nz.co.fortytwo.freeboard.server.util.Constants;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;

/**
 * Processes NMEA sentences in the body of a message, firing events to interested listeners
 * 
 * @author robert
 * 
 */
public class NMEAProcessor extends FreeboardProcessor implements Processor {

	private static final String DISPATCH_ALL = "DISPATCH_ALL";

	// map of sentence listeners
	private ConcurrentMap<String, List<SentenceListener>> listeners = new ConcurrentHashMap<String, List<SentenceListener>>();

	public NMEAProcessor() {
		setNmeaListeners();
	}

	public void process(Exchange exchange) throws Exception {
		if (StringUtils.isEmpty(exchange.getIn().getBody(String.class)))
			return;
		// so we have a string
		String bodyStr = exchange.getIn().getBody(String.class).trim();
		if (bodyStr.startsWith("$")) {
			try {
				Sentence sentence = SentenceFactory.getInstance().createParser(bodyStr);
				fireSentenceEvent(exchange, sentence);
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}

	/**
	 * Adds a {@link SentenceListener} that wants to receive all sentences read
	 * by the reader.
	 * 
	 * @param listener
	 *            {@link SentenceListener} to be registered.
	 * @see net.sf.marineapi.nmea.event.SentenceListener
	 */
	public void addSentenceListener(SentenceListener listener) {
		registerListener(DISPATCH_ALL, listener);
	}

	/**
	 * Adds a {@link SentenceListener} that is interested in receiving only
	 * sentences of certain type.
	 * 
	 * @param sl
	 *            SentenceListener to add
	 * @param type
	 *            Sentence type for which the listener is registered.
	 * @see net.sf.marineapi.nmea.event.SentenceListener
	 */
	public void addSentenceListener(SentenceListener sl, SentenceId type) {
		registerListener(type.toString(), sl);
	}

	/**
	 * Adds a {@link SentenceListener} that is interested in receiving only
	 * sentences of certain type.
	 * 
	 * @param sl
	 *            SentenceListener to add
	 * @param type
	 *            Sentence type for which the listener is registered.
	 * @see net.sf.marineapi.nmea.event.SentenceListener
	 */
	public void addSentenceListener(SentenceListener sl, String type) {
		registerListener(type, sl);
	}

	/**
	 * Remove a listener from reader. When removed, listener will not receive
	 * any events from the reader.
	 * 
	 * @param sl
	 *            {@link SentenceListener} to be removed.
	 */
	public void removeSentenceListener(SentenceListener sl) {
		for (List<SentenceListener> list : listeners.values()) {
			if (list.contains(sl)) {
				list.remove(sl);
			}
		}
	}

	/**
	 * Dispatch data to all listeners.
	 * 
	 * @param exchange
	 * 
	 * @param sentence
	 *            sentence string.
	 */
	private void fireSentenceEvent(Exchange exchange, Sentence sentence) {
		if (!sentence.isValid())
			return;

		String type = sentence.getSentenceId();
		Set<SentenceListener> list = new HashSet<SentenceListener>();

		if (listeners.containsKey(type)) {
			list.addAll(listeners.get(type));
		}
		if (listeners.containsKey(DISPATCH_ALL)) {
			list.addAll(listeners.get(DISPATCH_ALL));
		}

		for (SentenceListener sl : list) {
			try {
				SentenceEvent se = new SentenceEvent(exchange, sentence);
				sl.sentenceRead(se);
			} catch (Exception e) {
				// ignore listener failures
			}
		}

	}

	/**
	 * Registers a SentenceListener to hash map with given key.
	 * 
	 * @param type
	 *            Sentence type to register for
	 * @param sl
	 *            SentenceListener to register
	 */
	private void registerListener(String type, SentenceListener sl) {
		if (listeners.containsKey(type)) {
			listeners.get(type).add(sl);
		} else {
			List<SentenceListener> list = new Vector<SentenceListener>();
			list.add(sl);
			listeners.put(type, list);
		}
	}

	/**
	 * Adds NMEA sentence listeners to process NMEA to simple output
	 * 
	 * @param processor
	 */
	private void setNmeaListeners() {

		addSentenceListener(new SentenceListener() {

			public void sentenceRead(SentenceEvent evt) {
				Exchange exchange = (Exchange) evt.getSource();
				StringBuilder body = new StringBuilder();
				if (evt.getSentence() instanceof PositionSentence) {
					PositionSentence sen = (PositionSentence) evt.getSentence();
					if (sen.getPosition().getLatHemisphere() == CompassPoint.SOUTH) {
						appendValue(body,Constants.LAT,(0-sen.getPosition().getLatitude()));
					} else {
						appendValue(body,Constants.LAT,sen.getPosition().getLatitude());
					}
					if (sen.getPosition().getLonHemisphere() == CompassPoint.WEST) {
						appendValue(body,Constants.LON,(0-sen.getPosition().getLongitude()));
					} else {
						appendValue(body,Constants.LON,sen.getPosition().getLongitude());
					}
				}
				if (evt.getSentence() instanceof HeadingSentence) {
					HeadingSentence sen = (HeadingSentence) evt.getSentence();
					if (sen.isTrue()) {
						appendValue(body,Constants.COG,sen.getHeading());
					} else {
						appendValue(body,Constants.MGH,sen.getHeading());
					}
				}
				if (evt.getSentence() instanceof RMCSentence) {
					// ;
					RMCSentence sen = (RMCSentence) evt.getSentence();
					appendValue(body,Constants.SOG,sen.getSpeed());
				}
				if (evt.getSentence() instanceof VHWSentence) {
					// ;
					VHWSentence sen = (VHWSentence) evt.getSentence();
					appendValue(body,Constants.SOG,sen.getSpeedKnots());
					
					appendValue(body, Constants.MGH,sen.getMagneticHeading());
					appendValue(body, Constants.COG,sen.getHeading());

				}

				// MWV wind
				if (evt.getSentence() instanceof MWVSentence) {
					MWVSentence sen = (MWVSentence) evt.getSentence();
					if (sen.isTrue()) {
						appendValue(body, Constants.WDT,sen.getAngle());
						appendValue(body, Constants.WST,sen.getSpeed());
						appendValue(body, Constants.WSU,sen.getSpeedUnit());
						
					} else {
						appendValue(body, Constants.WDA,sen.getAngle());
						appendValue(body, Constants.WSA,sen.getSpeed());
						appendValue(body, Constants.WSU,sen.getSpeedUnit());
					}
				}
				
				exchange.getOut().setBody(body.toString());
			}


			public void readingStopped() {
			}

			public void readingStarted() {
			}

			public void readingPaused() {
			}
		});
	}

}
