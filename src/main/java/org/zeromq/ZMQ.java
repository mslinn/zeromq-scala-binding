package org.zeromq;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Offers an API similar to that of jzmq [1] written by Gonzalo Diethelm.
 *
 * 1. https://github.com/zeromq/jzmq
 */
public class ZMQ {
  private static Logger log = LoggerFactory.getLogger(ZMQ.class);
  private static final ZeroMQLibrary zmq = ZeroMQ$.MODULE$.loadLibrary();
  private static final int[] majorVersion = new int[1];
  private static final int[] minorVersion = new int[1];
  private static final int[] patchVersion = new int[1];

  public static final int NOBLOCK = ZeroMQ$.MODULE$.ZMQ_NOBLOCK();
  public static final int DONTWAIT = ZeroMQ$.MODULE$.ZMQ_NOBLOCK();
  public static final int PAIR = ZeroMQ$.MODULE$.ZMQ_PAIR();

  /** Indicates that a message has multiple frames and this is not the last frame */
  public static final int SNDMORE = ZeroMQ$.MODULE$.ZMQ_SNDMORE();
  public static final int PUB = ZeroMQ$.MODULE$.ZMQ_PUB();
  public static final int SUB = ZeroMQ$.MODULE$.ZMQ_SUB();
  public static final int REQ = ZeroMQ$.MODULE$.ZMQ_REQ();
  public static final int REP = ZeroMQ$.MODULE$.ZMQ_REP();
  public static final int XREQ = ZeroMQ$.MODULE$.ZMQ_DEALER();
  public static final int XREP = ZeroMQ$.MODULE$.ZMQ_ROUTER();
  public static final int DEALER = ZeroMQ$.MODULE$.ZMQ_DEALER();
  public static final int ROUTER = ZeroMQ$.MODULE$.ZMQ_ROUTER();
  public static final int PULL = ZeroMQ$.MODULE$.ZMQ_PULL();
  public static final int PUSH = ZeroMQ$.MODULE$.ZMQ_PUSH();
  public static final int STREAMER = ZeroMQ$.MODULE$.ZMQ_STREAMER();
  public static final int FORWARDER = ZeroMQ$.MODULE$.ZMQ_FORWARDER();
  public static final int QUEUE = ZeroMQ$.MODULE$.ZMQ_QUEUE();

  static {
    zmq.zmq_version(majorVersion, minorVersion, patchVersion);
  }

  public static int getMajorVersion() {
    return majorVersion[0];
  }

  public static int getMinorVersion() {
    return minorVersion[0];
  }

  public static int getPatchVersion() {
    return patchVersion[0];
  }

  public static int getFullVersion() {
    return makeVersion(getMajorVersion(), getMinorVersion(), getPatchVersion());
  }

  public static int makeVersion(int major, int minor, int patch) {
    return major * 10000 + minor * 100 + patch;
  }

  public static String getVersionString() {
    return String.format("%d.%d.%d", getMajorVersion(), getMinorVersion(), getPatchVersion());
  }

  public static Context context(int ioThreads) {
    return new Context(ioThreads);
  }

  public static class Context {
    protected Pointer ptr;

    public void term () {
    }

    public Socket socket(int type) {
      return new Socket(this, type);
    }

    public Poller poller() {
      return new Poller(this);
    }

    public Poller poller(int size) {
      return new Poller(this, size);
    }

    protected Context (int ioThreads) {
      ptr = zmq.zmq_init(ioThreads);
    }
  }

  public static class Socket {
    protected Pointer ptr;
    MessageDataBuffer messageDataBuffer = new MessageDataBuffer();

    public void close() {
      zmq.zmq_close(ptr);
    }

    public int getType() {
      if (getFullVersion() < makeVersion(2, 1, 0))
        return -1;
      return (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_TYPE());
    }

    public long getLinger() {
      if (getFullVersion() < makeVersion(2, 1, 0))
        return -1;
      return (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_LINGER());
    }

    public long getReconnectIVL() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL());
    }

    public long getBacklog() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_BACKLOG());
    }

    public long getReconnectIVLMax() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL_MAX());
    }

    public long getMaxMsgSize() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_MAXMSGSIZE());
    }

    public long getSndHWM() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDHWM());
    }

    public long getRcvHWM() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVHWM());
    }

    public long getHWM() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_HWM());
    }

    public long getSwap() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SWAP());
    }

    public long getAffinity() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_AFFINITY());
    }

    public byte[] getIdentity() {
      return getBytesSockopt(ZeroMQ$.MODULE$.ZMQ_IDENTITY());
    }

    public long getRate() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RATE());
    }

    public long getRecoveryInterval() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECOVERY_IVL());
    }

    public boolean hasMulticastLoop() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return false;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP()) != 0;
    }

    public void setMulticastHops(long mcast_hops) {
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP(), mcast_hops);
    }

    public long getMulticastHops() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP());
    }

    public void setReceiveTimeOut(long timeout) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVTIMEO(), timeout);
    }

    public long getReceiveTimeOut() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVTIMEO());
    }

    public void setSendTimeOut(long timeout) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDTIMEO(), timeout);
    }

    public long getSendTimeOut() {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDTIMEO());
    }

    public long getSendBufferSize() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDBUF());
    }

    public long getReceiveBufferSize() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVBUF());
    }

    public boolean hasReceiveMore() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVMORE()) != 0;
    }

    public long getFD() {
      if (getFullVersion() < makeVersion(2, 1, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_FD());
    }

    public long getEvents() {
      if (getFullVersion() < makeVersion(2, 1, 0))
        return -1;
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_EVENTS());
    }

    public void setLinger(long linger) {
      if (getFullVersion() < makeVersion(2, 1, 0))
        return;

      log.debug("Setting linger to " + linger);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_LINGER(), linger);
    }

    public void setReconnectIVL(long reconnectIVL) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;

      log.debug("Setting reconnectIVL to " + reconnectIVL);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL(), reconnectIVL);
    }

    public void setBacklog(long backlog) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;

      log.debug("Setting backlog to " + backlog);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_BACKLOG(), backlog);
    }

    public void setReconnectIVLMax(long reconnectIVLMax) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;

      log.debug("Setting reconnectIVLMax to " + reconnectIVLMax);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL_MAX(), reconnectIVLMax);
    }

    public void setMaxMsgSize(long maxMsgSize) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;

      log.debug("Setting maxMsgSize to " + maxMsgSize);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_MAXMSGSIZE(), maxMsgSize);
    }

    public void setSndHWM(long sndHWM) {
      if (getFullVersion() < makeVersion(3, 0, 0))
        return;

      log.debug("Setting sndHWM to " + sndHWM);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDHWM(), sndHWM);
    }

    public void setRcvHWM(long rcvHWM) {
      if (getFullVersion() >= makeVersion(3, 0, 0))
        return;

      log.debug("Setting rcvHWM to " + rcvHWM);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVHWM(), rcvHWM);
    }

    public void setHWM(long hwm) {
      if (getFullVersion() >= makeVersion(3, 0, 0))
        return;

      log.debug("Setting HWM to " + hwm);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_HWM(), hwm);
    }

    public void setSwap(long swap) {
      if (getFullVersion() >= makeVersion(3, 0, 0))
        return;
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SWAP(), swap);
    }

    public void setAffinity(long affinity) {
      log.debug("Setting affinity to " + affinity);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_AFFINITY(), affinity);
    }

    public void setIdentity(byte[] identity) {
      log.debug("Setting identity to '" + new String(identity) + "'");
      setBytesSockopt(ZeroMQ$.MODULE$.ZMQ_IDENTITY(), identity);
    }

    public void subscribe(byte[] topic) {
      log.debug("Subscribing to '" + new String(topic) + "'");
      setBytesSockopt(ZeroMQ$.MODULE$.ZMQ_SUBSCRIBE(), topic);
    }

    public void unsubscribe(byte[] topic) {
      log.debug("Unsubscribing from '" + new String(topic) + "'");
      setBytesSockopt(ZeroMQ$.MODULE$.ZMQ_UNSUBSCRIBE(), topic);
    }

    public void setRate (long rate) {
      log.debug("Setting rate to " + rate);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RATE(), rate);
    }

    public void setRecoveryInterval(long recovery_ivl) {
      log.debug("Setting recovery interval to " + recovery_ivl);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL(), recovery_ivl);
    }

    public void setMulticastLoop(boolean mcast_loop) {
      if (getFullVersion() >= makeVersion(3, 0, 0))
        return;

      log.debug("Setting multicast loop to " + mcast_loop);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP(), mcast_loop ? 1 : 0);
    }

    public void setSendBufferSize(long sndbuf) {
      log.debug("Setting send buffer size to " + sndbuf);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDBUF(), sndbuf);
    }

    public void setReceiveBufferSize(long rcvbuf) {
      log.debug("Setting receive buffer size to " + rcvbuf);
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVBUF(), rcvbuf);
    }

    public void bind(String addr) {
      log.debug("Binding to " + addr);
      zmq.zmq_bind(ptr, addr);
    }

    public void connect(String addr) {
      log.debug("Connecting to " + addr);
      zmq.zmq_connect(ptr, addr);
    }

      /**
       * Send a frame in blocking mode; if in non-blocking mode and the frame completes a message, send it
       * @param flags If set, the SNDMORE flag indicates that additional frames follow to complete the message
       * @see http://api.zeromq.org/2-1:zmq-send
       * @return true if successful
       * @throws ZMQException for any problem */
    public boolean send(byte[] msg, int flags) {
      log.debug("Entering ZMQ.send()");
      zmq_msg_t message = newZmqMessage(msg);
      if (zmq.zmq_send(ptr, message, flags) != 0) { // problem sending
        if (zmq.zmq_errno() == ZeroMQ$.MODULE$.EAGAIN()) {
          log.debug("  Non-blocking mode was requested and the message cannot be sent at the moment: '" +
                    new String(Arrays.copyOfRange(msg, 0, msg.length)) + "'");
          if (zmq.zmq_msg_close(message) != 0) {
            log.debug("  Problem closing ZMQ frame");
            raiseZMQException();
          } else {
            log.debug("  Frame not sent");
            return false;
          }
        } else {
          zmq.zmq_msg_close(message);
          raiseZMQException();
          return false;
        }
      }
      if (zmq.zmq_msg_close(message) != 0) {
        log.debug("  Problem closing ZMQ frame");
        raiseZMQException();
      }
      log.debug("  Message sent: '" + new String(Arrays.copyOfRange(msg, 7, msg.length+7)) + "'");
      return true;
    }

      /**
       * @see http://api.zeromq.org/2-1:zmq-recv
       * @return array of bytes containing received message if successful, or null if not
       * @throws ZMQException for any problem */
    public byte[] recv(int flags) {
      log.debug("Entering ZMQ.recv()");
      zmq_msg_t message = newZmqMessage();
      if (zmq.zmq_recv(ptr, message, flags) != 0) {
        if (zmq.zmq_errno() == ZeroMQ$.MODULE$.EAGAIN()) {
          log.debug("  Non-blocking mode was requested and no messages are available at the moment.");
          if (zmq.zmq_msg_close(message) != 0) {
            raiseZMQException();
          } else {
            return null;
          }
        } else {
          zmq.zmq_msg_close(message);
          log.debug("  Problem receiving message: '" + message + "'");
          raiseZMQException();
        }
      }
      Pointer data = zmq.zmq_msg_data(message);
      int length = zmq.zmq_msg_size(message);
      byte[] dataByteArray = data.getByteArray(0, length);
      if (zmq.zmq_msg_close(message) != 0) {
        log.debug("  Problem extracting data from message: '" + message + "'");
        raiseZMQException();
      }
      log.debug("  Message received: '" + new String(dataByteArray) + "'");
      return dataByteArray;
    }

    protected Socket(Context context, int type) {
      ptr = zmq.zmq_socket(context.ptr, type);
    }

    @Override protected void finalize() {
      close();
    }

    private long getLongSockopt(int option) {
      Memory value = new Memory(Long.SIZE / 8);
      LongByReference length = new LongByReference(Long.SIZE / 8);
      zmq.zmq_getsockopt(ptr, option, value, length);
      return value.getLong(0);
    }

    private void setLongSockopt(int option, long optval) {
      NativeLong length = new NativeLong(Long.SIZE / 8);
      Memory value = new Memory(Long.SIZE / 8);
      value.setLong(0, optval);
      zmq.zmq_setsockopt(ptr, option, value, length);
    }

    private byte[] getBytesSockopt(int option) {
      Memory value = new Memory(1024);
      LongByReference length = new LongByReference(1024);
      zmq.zmq_getsockopt(ptr, option, value, length);
      return value.getByteArray(0, (int) length.getValue());
    }

    private void setBytesSockopt(int option, byte[] optval) {
      NativeLong length = new NativeLong(optval.length);
      Pointer value = null;
      if (optval.length > 0) {
        value = new Memory(optval.length);
        value.write(0, optval, 0, optval.length);
      } else {
        value = Pointer.NULL;
      }
      zmq.zmq_setsockopt(ptr, option, value, length);
    }

    /** @throws ZMQException if a problem was encountered creating a new ZMQ Message */
    private zmq_msg_t newZmqMessage(byte[] msg) {
      zmq_msg_t message = new zmq_msg_t();
      if (msg.length == 0) {
        if (zmq.zmq_msg_init_size(message, new NativeLong(msg.length)) != 0) {
          raiseZMQException();
        }
      } else {
        Memory mem = new Memory(msg.length);
        mem.write(0, msg, 0, msg.length);
        if (zmq.zmq_msg_init_data(message, mem, new NativeLong(msg.length), messageDataBuffer, mem) == 0) {
          messageDataBuffer.add(mem);
        } else {
          raiseZMQException();
        }
      }
      return message;
    }

    private zmq_msg_t newZmqMessage() {
      zmq_msg_t message = new zmq_msg_t();
      if (zmq.zmq_msg_init(message) != 0)
        raiseZMQException();
      return message;
    }

    /** @throws ZMQException with reason, which is looked up from the zmq error number */
    private void raiseZMQException() {
      int errno = zmq.zmq_errno();
      String reason = zmq.zmq_strerror(errno);
      log.debug("Throwing ZMQException " + errno + ": " + reason);
      throw new ZMQException(reason, errno);
    }

    private class MessageDataBuffer implements zmq_free_fn {
      private HashSet<Pointer> buffer = new HashSet<Pointer>();

      public synchronized void add(Pointer data) {
        buffer.add(data);
      }

      public synchronized void invoke(Pointer data, Pointer memory) {
        buffer.remove(memory);
      }
    }
  }

  public static class Poller {
    public static final int POLLIN = ZeroMQ$.MODULE$.ZMQ_POLLIN();
    public static final int POLLOUT = ZeroMQ$.MODULE$.ZMQ_POLLOUT();
    public static final int POLLERR = ZeroMQ$.MODULE$.ZMQ_POLLERR();

    private static final int SIZE_DEFAULT = 32;
    private static final int SIZE_INCREMENT = 16;
    private static final int UNINITIALIZED_TIMEOUT = -2;

    private long timeout = UNINITIALIZED_TIMEOUT;
    private int nextEventIndex = 0;
    private int maxEventCount = 0;
    private int curEventCount = 0;
    private Socket[] sockets = null;
    private short[] events = null;
    private short[] revents = null;
    private LinkedList<Integer> freeSlots = null;

    public int register(Socket socket) {
      return register(socket, POLLIN | POLLOUT | POLLERR);
    }

    public int register(Socket socket, int numEvents) {
      int pos = -1;
      if (!freeSlots.isEmpty()) {
        pos = freeSlots.remove();
      } else {
        if (nextEventIndex >= maxEventCount) {
          int newMaxEventCount = maxEventCount + SIZE_INCREMENT;
          sockets = Arrays.copyOf(sockets, newMaxEventCount);
          events = Arrays.copyOf(events, newMaxEventCount);
          revents = Arrays.copyOf(revents, newMaxEventCount);
          maxEventCount = newMaxEventCount;
        }
        pos = nextEventIndex++;
      }
      sockets[pos] = socket;
      events[pos] = (short) numEvents;
      curEventCount++;
      return pos;
    }

    public void unregister(Socket socket) {
      for (int index = 0; index < nextEventIndex; index++) {
        if (sockets[index] == socket) {
          unregisterSocketAtIndex(index);
          break;
        }
      }
    }

    private void unregisterSocketAtIndex(int index) {
      sockets[index] = null;
      events[index] = 0;
      revents[index] = 0;
      freeSlots.add(index);
      curEventCount--;
    }

    public Socket getSocket(int index) {
      if (index < 0 || index >= nextEventIndex) {
          return null;
      }
      return sockets[index];
    }

    public long getTimeout() {
      return timeout;
    }

    public void setTimeout(long timeout) {
      this.timeout = timeout;
    }

    public int getSize() {
      return maxEventCount;
    }

    public int getNext() {
      return nextEventIndex;
    }

    public long poll() {
      long timeout = -1;
      if (this.timeout != UNINITIALIZED_TIMEOUT) {
        timeout = this.timeout;
      }
      return poll(timeout);
    }

    public long poll(long timeout) {
      int pollItemCount = 0;
      for (int i = 0; i < nextEventIndex; i++) {
        revents[i] = 0;
      }
      if (curEventCount == 0)
        return 0;
      zmq_pollitem_t[] items = (zmq_pollitem_t[]) new zmq_pollitem_t().toArray(curEventCount);
      for (int i = 0; i < pollItemCount; i++) {
        items[i] = new zmq_pollitem_t();
      }
      for (int socketIndex = 0; socketIndex < sockets.length; socketIndex++) {
        if (sockets[socketIndex] == null) {
          continue;
        }
        items[pollItemCount].socket = sockets[socketIndex].ptr;
        items[pollItemCount].fd = 0;
        items[pollItemCount].events = events[socketIndex];
        items[pollItemCount].revents = 0;
        pollItemCount++;
      }
      if (pollItemCount != curEventCount)
        return 0;
      pollItemCount = 0;
      int result = zmq.zmq_poll(items, curEventCount, new NativeLong(timeout));
      for (int socketIndex = 0; socketIndex < sockets.length; socketIndex++) {
        if (sockets[socketIndex] == null) {
          continue;
        }
        revents[socketIndex] = items[pollItemCount].revents;
        pollItemCount++;
      }
      return result;
    }

    public boolean pollin(int index) {
      return poll_mask(index, POLLIN);
    }

    public boolean pollout(int index) {
      return poll_mask(index, POLLOUT);
    }

    public boolean pollerr(int index) {
      return poll_mask(index, POLLERR);
    }

    protected Poller(Context context) {
      this(context, SIZE_DEFAULT);
    }

    protected Poller(Context context, int size) {
      this.maxEventCount = size;
      this.sockets = new Socket[maxEventCount];
      this.events = new short[maxEventCount];
      this.revents = new short[maxEventCount];
      this.freeSlots = new LinkedList<Integer>();
    }

    private boolean poll_mask (int index, int mask) {
      if (mask <= 0 || index < 0 || index >= nextEventIndex) {
        return false;
      }
      return (revents[index] & mask) > 0;
    }
  }
}
