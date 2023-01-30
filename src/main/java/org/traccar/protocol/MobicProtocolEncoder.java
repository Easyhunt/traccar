/*
 */
package org.traccar.protocol;

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

import io.netty.channel.Channel;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Protocol;

public class MobicProtocolEncoder extends StringProtocolEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobicProtocolEncoder.class);
    
    public MobicProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        String inSequenceId = MobicProtocolDecoder.DEFAULT_SEQUENCE_ID;
        String outSequenceId = MobicProtocolDecoder.DEFAULT_SEQUENCE_ID;
        if (channel != null) {
            inSequenceId = channel.pipeline().get(MobicProtocolDecoder.class).getSequenceId();
            outSequenceId = RandomStringUtils.random(5, false, true);
        }
        
        if (inSequenceId != null) {
            switch (command.getType()) {
                case Command.TYPE_CUSTOM:
                    return formatAlt(command, inSequenceId + outSequenceId + ",0|%s", Command.KEY_DATA);
                case Command.TYPE_POWER_OFF:
                    return formatAlt(command, inSequenceId + outSequenceId + ",0|PWROFF=EXECUTE");
                case Command.TYPE_REBOOT_DEVICE:
                    return formatAlt(command, inSequenceId + outSequenceId + ",0|RESETU=EXECUTE");
                case Command.TYPE_SET_CONNECTION:
                    return formatAlt(command, inSequenceId + outSequenceId + ",0|S1IPA1=%s", Command.KEY_SERVER);
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private String formatAlt(Command command, String format, String... keys) {
        String commandString = formatCommand(command, "$ASET00000" + format + "*00", keys);
        LOGGER.info("Mobic: deviceId={}; commandType={}; command={};", command.getDeviceId(), command.getType(), commandString);
        return commandString;
    }
    
}
