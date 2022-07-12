package me.jar.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import me.jar.constants.DecodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LengthContentDecoder extends ReplayingDecoder<DecodeState> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LengthContentDecoder.class);
    private int length = 0;
    public LengthContentDecoder() {
        super(DecodeState.LENGTH);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        switch (state()) {
            case LENGTH:
                length = in.readInt();
                checkpoint(DecodeState.CONTENT);
            case CONTENT:
                ByteBuf byteBuf = in.readBytes(length);
                checkpoint(DecodeState.LENGTH);
                out.add(byteBuf);
                break;
            default:
                LOGGER.error("===[LengthContentDecoder] - It is a illegal decoder state!");
        }
    }
}
