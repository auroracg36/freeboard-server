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

package nz.co.fortytwo.freeboard.server.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;


import org.apache.commons.lang3.StringUtils;

/**
 * Place for all the left over bits that are used across freeboard
 * @author robert
 *
 */
public class Util {
	
	private static Properties props;
	
	/**
	 * Smooth the data a bit
	 * @param prev
	 * @param current
	 * @return
	 */
	public static  double movingAverage(double ALPHA, double prev, double current) {
	    prev = ALPHA * prev + (1-ALPHA) * current;
	    return prev;
	}

	/**
	 * Load the config from the named dir, or if the named dir is false, from the default location
	 * The config is cached, subsequent calls get the same object 
	 * @param dir
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Properties getConfig(String dir) throws FileNotFoundException, IOException{
		if(props==null){
			props = new Properties();
			Util.setDefaults(props);
			if(StringUtils.isNotBlank(dir)){
				props.setProperty(Constants.CFG_DIR, dir);
			}
			File cfg = new File(props.getProperty(Constants.CFG_DIR)+props.getProperty(Constants.CFG_FILE));
			
			if(cfg.exists()){
				props.load(new FileReader(cfg));
			}
		}
		return props;
	}
	
	/**
	 * Save the current config to disk.
	 * @throws IOException
	 */
	public static void saveConfig() throws IOException{
		if(props==null)return;
		File cfg = new File(props.getProperty(Constants.CFG_DIR)+props.getProperty(Constants.CFG_FILE));
		props.store(new FileWriter(cfg), null);
		
	}

	/**
	 * Config defaults
	 * 
	 * @param props
	 */
	public static void setDefaults(Properties props) {
		//populate sensible defaults here
		props.setProperty(Constants.FREEBOARD_URL,"/freeboard");
		props.setProperty(Constants.FREEBOARD_RESOURCE,"freeboard/");
		props.setProperty(Constants.MAPCACHE_RESOURCE,"./mapcache");
		props.setProperty(Constants.MAPCACHE,"/mapcache");
		props.setProperty(Constants.HTTP_PORT,"8080");
		props.setProperty(Constants.WEBSOCKET_PORT,"9090");
		props.setProperty(Constants.CFG_DIR,"./conf/");
		props.setProperty(Constants.CFG_FILE,"freeboard.cfg");
		props.setProperty(Constants.DEMO,"false");
		props.setProperty(Constants.SERIAL_URL,"./src/test/resources/motu.log&scanStream=true&scanStreamDelay=500");
		props.setProperty(Constants.VIRTUAL_URL,"");
	}
	

	/**
	 * Round to specified decimals
	 * @param val
	 * @param places
	 * @return
	 */
	public static double round(double val, int places){
		double scale = Math.pow(10, places);
		long iVal = Math.round (val*scale);
		return iVal/scale;
	}
	
	/**
	 * Updates and saves the scaling values for instruments
	 * @param scaleKey
	 * @param amount
	 * @param scaleValue
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static double updateScale(String scaleKey, double amount, double scaleValue) throws FileNotFoundException, IOException {
			scaleValue = scaleValue*amount;
			scaleValue= Util.round(scaleValue, 2);
			//logger.debug(" scale now = "+scale);
			
			//write out to config
			Util.getConfig(null).setProperty(scaleKey, String.valueOf(scaleValue));
			Util.saveConfig();
			
		return scaleValue;
	}
}
