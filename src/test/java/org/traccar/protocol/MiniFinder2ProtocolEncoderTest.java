package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import io.netty.buffer.ByteBuf;

import static org.junit.Assert.assertEquals;

import org.apache.commons.codec.binary.Hex;

public class MiniFinder2ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeSetPositionPeriodic() throws Exception {

        Minifinder2ProtocolEncoder encoder = new Minifinder2ProtocolEncoder(null);

        Command command = new Command();

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 10);

        ByteBuf byteBuf = (ByteBuf) encoder.encodeCommand(null, command);
        String hexString = String.valueOf(Hex.encodeHex(byteBuf.array(), 0, 29, false));
        
        assertEquals("AB10150045F7000102050A000000030D44C80000800A000000100E0000", hexString);
    }
    
    @Test
    public void testEncodePowerOff() throws Exception {

        Minifinder2ProtocolEncoder encoder = new Minifinder2ProtocolEncoder(null);

        Command command = new Command();

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POWER_OFF);

        ByteBuf byteBuf = (ByteBuf) encoder.encodeCommand(null, command);
        String hexString = String.valueOf(Hex.encodeHex(byteBuf.array(), 0, 11, false));
        System.out.println(hexString);
        
        assertEquals("AB10030044BD0001040114", hexString);
    }

    @Test
    public void testEncodeReboot() throws Exception {

        Minifinder2ProtocolEncoder encoder = new Minifinder2ProtocolEncoder(null);

        Command command = new Command();

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        ByteBuf byteBuf = (ByteBuf) encoder.encodeCommand(null, command);
        String hexString = String.valueOf(Hex.encodeHex(byteBuf.array(), 0, 11, false));
        System.out.println(hexString);
        
        assertEquals("AB10030082DD0001040112", hexString);
    }
    
    @Test
    public void testEncodeSetTimeZone() throws Exception {

        Minifinder2ProtocolEncoder encoder = new Minifinder2ProtocolEncoder(null);

        Command command = new Command();

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+0");

        ByteBuf byteBuf = (ByteBuf) encoder.encodeCommand(null, command);
        String hexString = String.valueOf(Hex.encodeHex(byteBuf.array(), 0, 12, false));
        System.out.println(hexString);        
        
        assertEquals("AB1004005496000102020E30", hexString);
    }
    
    @Test
    public void testEncodeCustom() throws Exception {

        Minifinder2ProtocolEncoder encoder = new Minifinder2ProtocolEncoder(null);
        
        String customCommand = "AB100700F8F4000002050F630800C0";
        
        Command command = new Command();

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, customCommand);

        ByteBuf byteBuf = (ByteBuf) encoder.encodeCommand(null, command);
        String hexString = String.valueOf(Hex.encodeHex(byteBuf.array(), 0, 15, false));
        System.out.println(hexString);        
        
        assertEquals(customCommand, hexString);
    }
}
