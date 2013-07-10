/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *    
 *    This file incorporates work covered by the following copyright and  
 *    permission notice: 
 *    
 *		   	Copyright 2010 Guenther Hoelzl, Shawn Brown
 *
 *		   	This file is part of MINDdroid.
 *
 * 		  	MINDdroid is free software: you can redistribute it and/or modify
 * 		  	it under the terms of the GNU Affero General Public License as
 * 		  	published by the Free Software Foundation, either version 3 of the
 *   		License, or (at your option) any later version.
 *
 *   		MINDdroid is distributed in the hope that it will be useful,
 *   		but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   		GNU Affero General Public License for more details.
 *
 *   		You should have received a copy of the GNU Affero General Public License
 *   		along with MINDdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.robot.albert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import org.catrobat.catroid.R;
import org.catrobat.catroid.bluetooth.BTConnectable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

/**
 * This class is for talking to a LEGO NXT robot via bluetooth.
 * The communciation to the robot is done via LCP (LEGO communication protocol).
 * Objects of this class can either be run as standalone thread or controlled
 * by the owners, i.e. calling the send/recive methods by themselves.
 */
//@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH) just needed for debug output: nxtBTsocket.isConnected()
public class RobotAlbertBtCommunicator extends RobotAlbertCommunicator {

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// this is the only OUI registered by LEGO, see http://standards.ieee.org/regauth/oui/index.shtml

	private BluetoothAdapter btAdapter;
	private BluetoothSocket nxtBTsocket = null;
	private OutputStream nxtOutputStream = null;
	private InputStream nxtInputStream = null;

	private String mMACaddress;
	private BTConnectable myOwner;

	private boolean createThread = true; //just for testing

	public RobotAlbertBtCommunicator(BTConnectable myOwner, Handler uiHandler, BluetoothAdapter btAdapter,
			Resources resources) {
		super(uiHandler, resources);

		this.myOwner = myOwner;
		this.btAdapter = btAdapter;
	}

	public void setMACAddress(String mMACaddress) {
		this.mMACaddress = mMACaddress;
	}

	/**
	 * Creates the connection, waits for incoming messages and dispatches them. The thread will be terminated
	 * on closing of the connection.
	 */
	@Override
	public void run() {

		try {
			createNXTconnection();
		} catch (IOException e) {
		}

		while (connected) {

			Log.d("test","loop");
			try {
				receiveMessage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d("test","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!!!!!!!!!");
				e.printStackTrace();
			}
			/*
			 * Log.d("RobotAlbertBtComm", "run");
			 * try {
			 * receiveMessage();
			 * } catch (IOException e) {
			 * // TODO Auto-generated catch block
			 * Log.d("RobotAlbertBtComm", "Exception catched for receiveMessage");
			 * e.printStackTrace();
			 * }
			 */

			/*
			 * try {
			 * returnMessage = receiveMessage();
			 * if ((returnMessage.length >= 2)
			 * && ((returnMessage[0] == LCPMessage.REPLY_COMMAND) || (returnMessage[0] ==
			 * LCPMessage.DIRECT_COMMAND_NOREPLY))) {
			 * dispatchMessage(returnMessage);
			 * }
			 * 
			 * } catch (IOException e) {
			 * // don't inform the user when connection is already closed
			 * if (connected) {
			 * sendState(STATE_RECEIVEERROR);
			 * }
			 * return;
			 * }
			 */
		}
	}

	/**
	 * Create a bluetooth connection with SerialPortServiceClass_UUID
	 * 
	 * @see <a href=
	 *      "http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android"
	 *      />
	 *      On error the method either sends a message to it's owner or creates an exception in the
	 *      case of no message handler.
	 */
	@Override
	public void createNXTconnection() throws IOException {
		try {
			BluetoothSocket nxtBTSocketTemporary;
			BluetoothDevice nxtDevice = null;
			nxtDevice = btAdapter.getRemoteDevice(mMACaddress);
			if (nxtDevice == null) {
				if (uiHandler == null) {
					throw new IOException();
				} else {
					sendToast(mResources.getString(R.string.no_paired_nxt));
					sendState(STATE_CONNECTERROR);
					return;
				}
			}

			nxtBTSocketTemporary = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
			try {

				nxtBTSocketTemporary.connect();

			} catch (IOException e) {
				if (myOwner.isPairing()) {
					if (uiHandler != null) {
						sendToast(mResources.getString(R.string.pairing_message));
						sendState(STATE_CONNECTERROR_PAIRING);
					} else {
						throw e;
					}
					return;
				}

				// try another method for connection, this should work on the HTC desire, credits to Michael Biermann
				try {

					Method mMethod = nxtDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
					nxtBTSocketTemporary = (BluetoothSocket) mMethod.invoke(nxtDevice, Integer.valueOf(1));
					nxtBTSocketTemporary.connect();
				} catch (Exception e1) {
					if (uiHandler == null) {
						throw new IOException();
					} else {
						sendState(STATE_CONNECTERROR);
					}
					return;
				}
			}
			nxtBTsocket = nxtBTSocketTemporary;
			nxtInputStream = nxtBTsocket.getInputStream();
			nxtOutputStream = nxtBTsocket.getOutputStream();
			connected = true;
		} catch (IOException e) {
			if (uiHandler == null) {
				throw e;
			} else {
				if (myOwner.isPairing()) {
					sendToast(mResources.getString(R.string.pairing_message));
				}
				sendState(STATE_CONNECTERROR);
				return;
			}
		}
		// everything was OK
		if (uiHandler != null) {
			sendState(STATE_CONNECTED);
		}
	}

	/**
	 * Closes the bluetooth connection. On error the method either sends a message
	 * to it's owner or creates an exception in the case of no message handler.
	 */
	@Override
	public void destroyNXTconnection() throws IOException {

		Log.d("RobotAlbertBtComm", "destroyRobotAlbertConnection");

		if (connected) {
			stopAllNXTMovement();
		}

		try {
			if (nxtBTsocket != null) {
				connected = false;
				nxtBTsocket.close();
				nxtBTsocket = null;
			}

			nxtInputStream = null;
			nxtOutputStream = null;

		} catch (IOException e) {
			if (uiHandler == null) {
				throw e;
			} else {
				sendToast(mResources.getString(R.string.problem_at_closing));
			}
		}
	}

	@Override
	public void stopAllNXTMovement() {
		myHandler.removeMessages(0);
		myHandler.removeMessages(1);
		myHandler.removeMessages(2);

		/*
		 * moveMotor(0, 0, 0);
		 * moveMotor(1, 0, 0);
		 * moveMotor(2, 0, 0);
		 */
		resetRobotAlbert();

	}

	/**
	 * Sends a message on the opened OutputStream
	 * 
	 * @param message
	 *            , the message as a byte array
	 */
	@Override
	public void sendMessage(byte[] message) throws IOException {

		try {

			Log.d("RobotAlbertBTComm", "sendMessage: nxtOutputstream=" + nxtOutputStream);
			Log.d("RobotAlbertBTComm", "sendMessage: nxtBtSocket=" + nxtBTsocket);
			//Log.d("RobotAlbertBTComm", "sendMessage: nxtBtSocketisConnected=" + nxtBTsocket.isConnected());

			if (nxtOutputStream == null) {
				throw new IOException();
			}

			// send message length
			//int messageLength = message.length;
			//nxtOutputStream.write(messageLength);
			//nxtOutputStream.write(messageLength >> 8);
			nxtOutputStream.write(message, 0, message.length);
			nxtOutputStream.flush();

			//Log.d("test", "Text=" + receiveMessage());
			if (createThread == true) {
				//createThread = false;
				Log.d("test", "Thread created");
				/*
				 * Thread thread1 = new Thread() {
				 * 
				 * @Override
				 * public void run() {
				 * try {
				 * while (true) {
				 * Log.d("test", "Thread starts from the beginning");
				 * receiveMessage();
				 * }
				 * } catch (Exception e) {
				 * e.printStackTrace();
				 * Log.d("Error in Thread:", " " + e);
				 * }
				 * }
				 * };
				 * thread1.start();
				 */
				//receiveMessage();
				Log.d("test", "after receive message");
			}

		} catch (Exception e) {
			Log.d("RobotAlbertBtComm", "ERROR: Exception occured in sendMessage " + e.getMessage());
		}
	}

	/**
	 * Receives a message on the opened InputStream
	 * 
	 * @return the message
	 */
	@Override
	public byte[] receiveMessage() throws IOException {

		Log.d("RobotAlbertBtComm", "receiveMessage InputStream=" + nxtInputStream);
		if (nxtInputStream == null) {
			throw new IOException();
		}

		//		int length = nxtInputStream.read();
		//		length = (nxtInputStream.read() << 8) + length;
		//		byte[] returnMessage = new byte[length];
		//		nxtInputStream.read(returnMessage);
		//		Log.i("bt", returnMessage.toString());
		byte[] buffer = new byte[100];

		/*
		 * while (nxtInputStream.available() ) {
		 * }
		 */

		int read = 0;

		/*read = nxtInputStream.read(buffer);
		Log.d("RobotAlbertBtComm", "receiveMessage: read=" + read);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[0]=" + buffer[0]);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[1]=" + buffer[1]);
		if (read > 2) {
			Log.d("RobotAlbertBtComm", "receiveMessage: buffer[" + (read - 2) + "]=" + buffer[read - 2]);
			Log.d("RobotAlbertBtComm", "receiveMessage: buffer[" + (read - 1) + "]=" + buffer[read - 1]);
			int e = buffer[0];
			Log.d("test","test="+e);
		}*/

		byte[] buf = new byte[1];
		byte[] buf0 = new byte[2];
		int count2=0;
		
		do
		{
			//Log.d("test","checking 0xAA");
			read = nxtInputStream.read(buf0);
			count2++;
			
			if(count2 > 200)
				return null;
			
		}while((buf0[0] != -86) || (buf0[1] != 85));
		
		int count = 2;
		buffer[0] = buf0[0];
		buffer[1] = buf0[1];
		
		do
		{
			read = nxtInputStream.read(buf);
			buffer[count] = buf[0];
			count++;
			//Log.d("test", "waiting for 0x0A (count="+count+")");
		}while( (buffer[count] != 13) && (buffer[count-1] != 10) );
		
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[13]=" + buffer[13]);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[14]=" + buffer[14]);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[15]=" + buffer[15]);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[16]=" + buffer[16]);
		//Log.d("RobotAlbertBtComm", "receiveMessage: buffer[17]=" + buffer[17]);
		//Log.d("RobotAlbertBtComm", "receiveMessage: buffer[18]=" + buffer[18]);
		//Log.d("RobotAlbertBtComm", "receiveMessage: buffer[19]=" + buffer[19]);
		//Log.d("RobotAlbertBtComm", "receiveMessage: buffer[20]=" + buffer[20]);
		
		
		/*read = nxtInputStream.read(buffer);
		Log.d("RobotAlbertBtComm", "receiveMessage: read=" + read);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[0]=" + buffer[0]);
		Log.d("RobotAlbertBtComm", "receiveMessage: buffer[1]=" + buffer[1]);
		if (read > 2) {
			Log.d("RobotAlbertBtComm", "receiveMessage: buffer[" + (read - 2) + "]=" + buffer[read - 2]);
			Log.d("RobotAlbertBtComm", "receiveMessage: buffer[" + (read - 1) + "]=" + buffer[read - 1]);
			int e = buffer[0];
			Log.d("test","test="+e);
		}*/
		
		
		/*
		 * boolean loop = true;
		 * boolean loop2 = true;
		 * int count = 2;
		 * int temp = 0;
		 * while (loop == true) {
		 * int byte0 = nxtInputStream.read();
		 * int byte1 = 0;
		 * if ((byte0 == -86)) {
		 * Log.d("test", "byte0 = -86");
		 * byte1 = nxtInputStream.read();
		 * if (((byte) byte1 == 85)) {
		 * Log.d("test", "byte1 = 85");
		 * count = 2;
		 * buffer[0] = (byte) byte0;
		 * buffer[1] = (byte) byte1;
		 * do {
		 * Log.d("test", "loop2 (count=" + count + ")");
		 * temp = (byte) nxtInputStream.read();
		 * if (temp != -1) {
		 * buffer[count] = (byte) temp;
		 * count++;
		 * } else {
		 * Log.d("RobotAlbertBtComm", "read returned -1");
		 * }
		 * 
		 * if ((buffer[count - 2] == 13) && (buffer[count - 1] == 10)) {
		 * Log.d("RobotAlbertBtComm", "End of sensor-packet reached");
		 * loop2 = false;
		 * loop = false;
		 * }
		 * 
		 * if (count > 60) {
		 * Log.d("RobotAlbertBtComm", "Error: sensor-packet bigger than 60 bytes!!!!!");
		 * }
		 * 
		 * } while (loop2 == true);
		 * }
		 * } else {
		 * Log.d("test", "byte0 != -86 (" + byte0 + ") -> loop");
		 * continue;
		 * }
		 * 
		 * }
		 */

		//Log.d("RobotAlbertBtComm", "receiveMessage: buffer[read]=" + buffer[read]);

		/*
		 * if (nxtInputStream.available() > 0) {
		 * byte a = 0x00;
		 * do {
		 * int first = nxtInputStream.read();
		 * if (first != -1) {
		 * a = (byte) first;
		 * Log.d("RobotAlbertBtComm", "receiveMessage: a=" + a);
		 * }
		 * } while (a != 0xAA);
		 * int second = nxtInputStream.read();
		 * if (second != -1) {
		 * byte b = (byte) second;
		 * Log.d("RobotAlbertBtComm", "receiveMessage: b=" + b);
		 * }
		 * 
		 * }
		 */

		//Log.d("RobotAlbertBtCommunicator", "something received:" + buffer.toString());
		return buffer;
	}
}
