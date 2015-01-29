package ru.artem.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Client {
    private InetSocketAddress address;
    private Selector selector;

    public Client(InetSocketAddress address) throws IOException {
        this.address = address;
        startClient();
    }

    private void startClient() throws IOException {
        // create selector and channel
        this.selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);
        channel.register(this.selector, SelectionKey.OP_CONNECT);

        log("Echo client ready. Ctrl-C to stop.");

        // processing
        while (true) {
            // wait for events
            this.selector.select();

            // wakeup to work on selected keys
            Iterator keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();

                // this is necessary to prevent the same key from coming up
                // again the next time around.
                keys.remove();

                if (! key.isValid()) {
                    continue;
                }

                if (key.isConnectable()) {
                    this.connect(key);
                }
                else if (key.isReadable()) {
                    this.read(key);
                }
                else if (key.isWritable()) {
                    this.write(key);
                }
            }
        }
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()){
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_WRITE);
        log("Connected to: " + channel.getRemoteAddress().toString());
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int numRead = -1;
        try {
            numRead = channel.read(buffer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (numRead == -1) {
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            log("Connection closed by server: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        log("Got: " + new String(data));
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.wrap("Hello from client".getBytes()));
        key.interestOps(SelectionKey.OP_READ);
    }

    private static void log(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) throws Exception {
        new Client(new InetSocketAddress("127.0.0.1", 5000));
    }
}
