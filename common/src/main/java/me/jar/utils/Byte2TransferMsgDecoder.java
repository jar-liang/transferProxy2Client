package me.jar.utils;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.jar.constants.TransferMsgType;
import me.jar.message.TransferMsg;

import java.util.List;
import java.util.Map;

public class Byte2TransferMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int type = msg.readInt();
        TransferMsgType transferMsgType = TransferMsgType.toTransferMsgType(type);

        int metaDataLength = msg.readInt();
        byte[] metaDataBytes = new byte[metaDataLength];
        msg.readBytes(metaDataBytes);
        Map<String, Object> metaData = JSON.parseObject(metaDataBytes, Map.class);

        byte[] data = null;
        if (msg.isReadable()) {
            data = ByteBufUtil.getBytes(msg);
        }

        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(transferMsgType);
        transferMsg.setMetaData(metaData);
        transferMsg.setDate(data);

        out.add(transferMsg);
    }
}
