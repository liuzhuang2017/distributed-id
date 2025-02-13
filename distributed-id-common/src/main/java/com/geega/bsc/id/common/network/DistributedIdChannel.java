package com.geega.bsc.id.common.network;

import cn.hutool.core.builder.HashCodeBuilder;
import com.geega.bsc.id.common.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * @author Jun.An3
 * @date 2022/07/18
 */
@Slf4j
public class DistributedIdChannel {

    private final String id;

    private final TransportLayer transportLayer;

    private final int maxReceiveSize;

    private NetworkReceive receive;

    private Send send;

    public DistributedIdChannel(String id, TransportLayer transportLayer, int maxReceiveSize) {
        this.id = id;
        this.transportLayer = transportLayer;
        this.maxReceiveSize = maxReceiveSize;
    }

    public void close() throws IOException {
        ByteUtil.closeAll(transportLayer);
    }


    public boolean finishConnect() throws IOException {
        return transportLayer.finishConnect();
    }

    public String id() {
        return id;
    }

    public void removeReadEvent() {
        transportLayer.removeInterestOps(SelectionKey.OP_READ);
    }

    public void interestReadEvent() {
        transportLayer.addInterestOps(SelectionKey.OP_READ);
    }

    public boolean isReadEvent() {
        return !transportLayer.isMute();
    }

    public String socketDescription() {
        Socket socket = transportLayer.socketChannel().socket();
        if (socket.getInetAddress() == null) {
            return socket.getLocalAddress().toString();
        }
        return socket.getInetAddress().toString();
    }

    public boolean setSend(Send send) {
        if (this.send != null) {
            return false;
        }
        this.send = send;
        this.transportLayer.addInterestOps(SelectionKey.OP_WRITE);
        return true;
    }

    public NetworkReceive read() throws IOException {
        NetworkReceive result = null;

        if (receive == null) {
            receive = new NetworkReceive(maxReceiveSize, id);
        }

        receive(receive);
        if (receive.complete()) {
            receive.payload().rewind();
            result = receive;
            receive = null;
        }
        return result;
    }

    public Send write() throws IOException {
        Send result = null;
        if (send != null && send(send)) {
            result = send;
            send = null;
        }
        return result;
    }

    private void receive(NetworkReceive receive) throws IOException {
        receive.readFrom(transportLayer);
    }

    private boolean send(Send send) throws IOException {
        send.writeTo(transportLayer);
        if (send.completed()) {
            //写完数据，要去掉写事件
            transportLayer.removeInterestOps(SelectionKey.OP_WRITE);
        }
        return send.completed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(this.id, ((DistributedIdChannel) o).id);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).toHashCode();
    }

}