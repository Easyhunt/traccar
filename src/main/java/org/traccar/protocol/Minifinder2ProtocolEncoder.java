/*
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

public class Minifinder2ProtocolEncoder extends BaseProtocolEncoder {

    public static final int COMMAND_HEADER = 0xab;
    public static final int COMMAND_SYSTEM = 0x04;
    public static final int COMMAND_SYSTEM_REBOOT_LENGTH  = 0x01;
    public static final int COMMAND_SYSTEM_REBOOT_KEY  = 0x12;
    public static final int COMMAND_SYSTEM_POWER_OFF_LENGTH  = 0x01;
    public static final int COMMAND_SYSTEM_POWER_OFF_KEY  = 0x14;

    public static final int COMMAND_CONFIGURATION = 0x02;
    public static final int COMMAND_CONFIGURATION_TIME_INTERVAL_LENGTH  = 0x0d;
    public static final int COMMAND_CONFIGURATION_TIME_INTERVAL_KEY  = 0x44;
    
    public static final int COMMAND_CONFIGURATION_TIME_ZONE_LENGTH  = 0x02;
    public static final int COMMAND_CONFIGURATION_TIME_ZONE_KEY  = 0x0E;

    public static final int COMMAND_CONFIGURATION_WORK_MODE_LENGTH  = 0x05;
    public static final int COMMAND_CONFIGURATION_WORK_MODE_KEY  = 0x0A;
    
    public static final int COMMAND_CONFIGURATION_OUTPUT_CONTROL_LENGTH  = 0x05;
    public static final int COMMAND_CONFIGURATION_OUTPUT_CONTROL_KEY  = 0x0f;
    public static final float COMMAND_CONFIGURATION_OUTPUT_CONTROL_DATA  = 0x630800C0;
    
    public Minifinder2ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        ByteBuf message = Unpooled.buffer();

        if (command.getType().equals(Command.TYPE_CUSTOM)) {
            String commandString = command.getString(Command.KEY_DATA); 
            byte[] content = DataConverter.parseHex(commandString);
            message.writeBytes(content);
        } else {
            int sequenceId = Minifinder2ProtocolDecoder.DEFAULT_SEQUENCE_ID;
            if (channel != null) {
                sequenceId = channel.pipeline().get(Minifinder2ProtocolDecoder.class).getSequenceId();
            }

            ByteBuf content = Unpooled.buffer();
            switch (command.getType()) {
                case Command.TYPE_REBOOT_DEVICE:
                    content.writeByte(COMMAND_SYSTEM);
                    content.writeByte(COMMAND_SYSTEM_REBOOT_LENGTH);    // key length
                    content.writeByte(COMMAND_SYSTEM_REBOOT_KEY);       // key
                    break;
                case Command.TYPE_POWER_OFF:
                    content.writeByte(COMMAND_SYSTEM);
                    content.writeByte(COMMAND_SYSTEM_POWER_OFF_LENGTH); // key length
                    content.writeByte(COMMAND_SYSTEM_POWER_OFF_KEY);    // key
                    break;
                 case Command.TYPE_SET_TIMEZONE:
                     content.writeByte(COMMAND_CONFIGURATION);
                     content.writeByte(COMMAND_CONFIGURATION_TIME_ZONE_LENGTH); // key length
                     content.writeByte(COMMAND_CONFIGURATION_TIME_ZONE_KEY);    // key
                     int offset = TimeZone.getTimeZone(command.getString(Command.KEY_TIMEZONE)).getRawOffset() / (1000 * 60 * 15);
                     content.writeBytes(String.valueOf(offset).getBytes(StandardCharsets.US_ASCII));
                     break;
                 case Command.TYPE_POSITION_PERIODIC:
                     content.writeByte(COMMAND_CONFIGURATION);
                     
                     content.writeByte(COMMAND_CONFIGURATION_WORK_MODE_LENGTH); // key length
                     content.writeByte(COMMAND_CONFIGURATION_WORK_MODE_KEY);    // key
                     content.writeMediumLE(0);  // Mode = 1/2/3, value is not used
                                                // Mode = 4, value is GSM/GPS Work Interval 60*60, 10*60-60*60*24*7
                                                // Mode = 5, value is GSM/GPS Work Time 30*60, 60-60*60
                     content.writeByte(3);      // 1,1-5
                     
                     content.writeByte(COMMAND_CONFIGURATION_TIME_INTERVAL_LENGTH); // key length
                     content.writeByte(COMMAND_CONFIGURATION_TIME_INTERVAL_KEY);    // key
                     content.writeIntLE(-2147483448); // Heart Rate Interval:  Bit31:1 enable 1 Unit: second, 200,10-3600*24                     

                     content.writeIntLE(command.getInteger(Command.KEY_FREQUENCY)); // Auto Upload Interval:  Unit: second, 180, >10s
                     
                     content.writeIntLE(60*60); // Auto Upload Lazy Interval:  Unit: second, 600, >300s
                     break;
                 default:
                     return null;
             }
            
            message.writeByte(COMMAND_HEADER); // header
            message.writeByte(0x10); // properties
            message.writeShortLE(content.readableBytes());  
            message.writeShortLE(Checksum.crc16(Checksum.CRC16_XMODEM, content.nioBuffer()));
            message.writeShortLE(sequenceId); //Sequence
            message.writeBytes(content);
            content.release();
        }
        
        return message;
    }

}
