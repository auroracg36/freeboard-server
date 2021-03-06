/*
 * Copyright 2012,2013 Robert Huitema robert@42.co.nz
 *
 * This file is part of FreeBoard. (http://www.42.co.nz/freeboard)
 *
 *  FreeBoard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  FreeBoard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with FreeBoard.  If not, see <http://www.gnu.org/licenses/>.
 */
package nz.co.fortytwo.freeboard.zk;

import java.io.FileNotFoundException;
import java.io.IOException;
import nz.co.fortytwo.freeboard.server.CamelContextFactory;

import nz.co.fortytwo.freeboard.server.util.Constants;
import nz.co.fortytwo.freeboard.server.util.Util;
import org.apache.camel.ProducerTemplate;

import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

public class DepthViewModel extends SelectorComposer<Window>{

	private static Logger logger = Logger.getLogger(DepthViewModel.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@WireVariable
        private Session sess;

	@Wire ("#depth")
	Window depth;

	@Wire ("#depthScale")
	Label depthScale;

	@Wire ("#depthUnit")
	Label depthUnit;

	@Wire ("#alarmDepth")
	Label alarmDepth;

	@Wire ("button#depthShrink")
	Button depthShrink;

	@Wire ("button#depthGrow")
	Button depthGrow;

	private double scale=0.7;

	private ProducerTemplate producer;

	public DepthViewModel() {
		super();
		logger.debug("Constructing..");
		producer = CamelContextFactory.getInstance().createProducerTemplate();
		producer.setDefaultEndpointUri("seda:input");
	}

	@Override
	public void doAfterCompose(Window comp) throws Exception {
		super.doAfterCompose(comp);
                Clients.evalJavaScript("depthOnLoad();");

			depthUnit.setValue(Util.getConfig(null).getProperty(Constants.DEPTH_UNIT));
                        alarmDepth.setValue(Util.getConfig(null).getProperty(Constants.ALARM_DEPTH));
			producer.sendBody(Constants.UID+":"+Constants.MEGA+","+Constants.DEPTH_ZERO_ADJUST_CMD+":"+Util.getConfig(null).getProperty(Constants.DEPTH_ZERO_OFFSET)+",");
	}

	@Listen("onMove =  #depth")
	public void onMoveWindow(Event event) {
	}

	@Listen("onClick =  button#depthShrink")
	public void logShrinkClick(MouseEvent event) {
	}

	@Listen("onClick =   button#depthGrow")
	public void logGrowClick(MouseEvent event) {
	}
}
