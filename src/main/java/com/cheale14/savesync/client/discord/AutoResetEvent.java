package com.cheale14.savesync.client.discord;

/*

 * Taken from 
 * https://stackoverflow.com/a/13838191/5131787
 */
public class AutoResetEvent
{
    private final Object _monitor = new Object();
    private volatile boolean _isOpen = false;
    public IPCFramePacket packet;

    public AutoResetEvent(boolean open)
    {
        _isOpen = open;
    }

    public void waitOne() throws InterruptedException
    {
        synchronized (_monitor) {
            while (!_isOpen) {
                _monitor.wait();
            }
            _isOpen = false;
        }
    }

    public void waitOne(long timeout) throws InterruptedException
    {
        synchronized (_monitor) {
            long t = System.currentTimeMillis();
            while (!_isOpen) {
                _monitor.wait(timeout);
                // Check for timeout
                if (System.currentTimeMillis() - t >= timeout)
                    break;
            }
            _isOpen = false;
        }
    }

    public void set(IPCFramePacket ipacket)
    {
        synchronized (_monitor) {
            _isOpen = true;
            packet = ipacket;
            _monitor.notify();
        }
    }

    public void reset()
    {
        _isOpen = false;
    }
}
