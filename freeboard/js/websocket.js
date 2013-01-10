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
var _ws;
var wsList = [];

function initSocket(){
	//make a web socket
	if(this._ws == null) {
		var location = "ws://"+window.location.hostname+":9090/navData";
		//alert(location);
		
		this._ws = new WebSocket(location);
		this._ws.onopen = function() {
		};
		this._ws.onmessage = function(m) {
			//iterate the array and process each, avoid NMEA for now
			if(m.data.trim().startsWith('$'))return;
			jQuery.each(wsList, function(i, obj) {
			      obj.onmessage(m);
			    });
		};
		this._ws.onclose = function() {
			this._ws = null;
		};
	}
}
