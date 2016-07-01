
/**
 * OTClock.java
 * Prakash Manandhar and Arun Ghimire
 * jsaaymi@gmail.com, arun.pg@hotmail.com
 *
 * Open Talking Clock is an open source java application that runs in as 
 * many mobile platforms as possible and presents a simple interface that
 * speaks out the current time.
 *
 * @version 1.0
 */

import java.io.*;

import java.util.Calendar;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.midlet.MIDlet;

public class OTClock extends MIDlet implements CommandListener, Runnable, PlayerListener 
{
    private static final String res_folder = "/res-np/";
    private static final String FORMAT = "audio/x-wav";
    private static final String EXTENSION = ".wav";
    //private static final String res_folder = "/res-np-mp3/";
    //private static final String FORMAT = "audio/mpeg";
    //private static final String EXTENSION = ".wav.mp3";

    private Player player;
    private Thread dThread;
    private Object dThreadLock = new Object();
    private Object dPlayLock = new Object();

    private InputStream is_arr[] = new InputStream[10];
    private int is_index;
    private int is_index_max;

    public void startApp() {
	Display display = Display.getDisplay(this);
	Form mainForm = new Form("OTClock");
	Calendar cal = Calendar.getInstance();
	int hour = cal.get(Calendar.HOUR_OF_DAY);
	int minute = cal.get(Calendar.MINUTE);
	String display_str = "Now: " + hour + ":" + minute;
	mainForm.append(display_str);
	int am_pm;
	if (hour < 12) {
	    am_pm = Calendar.AM;
	    if (hour == 0) hour = 12;
	}
	else {
	    am_pm = Calendar.PM;
	    if (hour > 12) hour = hour - 12;
	}
	is_index = 0;
	is_index_max = 0;
	playTime (hour, minute, am_pm);
	Command exitCommand = new Command("Exit", Command.EXIT, 0);
	mainForm.addCommand(exitCommand);
	mainForm.setCommandListener(this);
	display.setCurrent(mainForm);
    }

private void playTime (int hour, int minute, int am_pm)
    {
	try {
	    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "pre" + EXTENSION);
	    if (am_pm == Calendar.AM)
	    {
		if (hour == 12 || (hour >= 1 && hour < 4))
		    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "pren" + EXTENSION);
		else if (hour >= 4 && hour < 10)
		    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "prem" + EXTENSION);
		else
		    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "prea" + EXTENSION);
	    }
	    else
	    {
		if (hour == 12 || (hour >= 1 && hour < 4))
		    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "prea" + EXTENSION);
		else if (hour >= 4 && hour < 9)
		    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "pree" + EXTENSION);
		else
		    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "pren" + EXTENSION);
	    }
	    is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + hour + EXTENSION);
	    if (minute == 0) {
		is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "post" + EXTENSION);
	    }
	    else
	    { 
		is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + minute + EXTENSION);
		is_arr[is_index_max++] = getClass().getResourceAsStream(res_folder + "postm" + EXTENSION);
	    }
	    playStreamList ();
	} catch (Exception e)
	{
	    e.printStackTrace();
	}	
    }

    private void playStreamList ()
    {
	// start new player
        synchronized (dThreadLock) {
            dThread = new Thread(this);
            dThread.start();
        }

    }

    private void playStream (int i) throws Exception
    {
       /*
         * method playSound() runs on GUI thread.
         * Manager.createPlayer() will potentially invoke a blocking
         * I/O. This is not the good practice recommended by MIDP
         * programming style. So here we will create the
         * Player in a separate thread.
         */
        player = Manager.createPlayer(is_arr[i], FORMAT);
	player.addPlayerListener (this);

        if (player == null) {
            // can't create player
            synchronized (dThreadLock) {
                dThread = null;
                dThreadLock.notify();

                return;
            }
        }

        try {
            player.prefetch();
	    //player.setMediaTime(player.getDuration());
            player.start();
	    synchronized(dPlayLock)
	    {
		dPlayLock.wait();
	    }
        } catch (Exception ex) {
		ex.printStackTrace();
        }
	
	dThread.sleep(300);
        // terminating player and the thread
        player.close();
        player = null;
    }

    public void run() {
	for (int i = 0; i < is_index_max; ++i)
	{
		try {
		    playStream(i);
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
 
        synchronized (dThreadLock) {
            dThread = null;
            dThreadLock.notify();
        }
	destroyApp(false);
	notifyDestroyed();
    }
    
    public void pauseApp () {}
    
    public void destroyApp(boolean unconditional) {}
    
    public void commandAction(Command c, Displayable s) {
	if (c.getCommandType() == Command.EXIT)
		notifyDestroyed();
    }

    public void playerUpdate (Player player, String event, Object eventData)
    {
	if (event == PlayerListener.END_OF_MEDIA)
	{
	    synchronized (dPlayLock) {
		dPlayLock.notify();
	    }
	}
    }
}

