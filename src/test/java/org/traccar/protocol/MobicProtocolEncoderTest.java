package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class MobicProtocolEncoderTest extends ProtocolTest {

    private String commandHeader = "$ASET00000" + MobicProtocolDecoder.DEFAULT_SEQUENCE_ID + MobicProtocolDecoder.DEFAULT_SEQUENCE_ID + ",0|";

    @Test
    public void testEncodePowerOff() {

        MobicProtocolEncoder encoder = new MobicProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POWER_OFF);

        assertEquals(commandHeader + "PWROFF=EXECUTE*00", encoder.encodeCommand(null, command));
    }

    @Test
    public void testEncodeRebootDevice() {

        MobicProtocolEncoder encoder = new MobicProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        assertEquals(commandHeader + "RESETU=EXECUTE*00", encoder.encodeCommand(null, command));
    }

    @Test
    public void testEncodeCustom() {

        MobicProtocolEncoder encoder = new MobicProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "TEST=TEST");

        assertEquals(commandHeader + "TEST=TEST*00", encoder.encodeCommand(null, command));
    }

    @Test
    public void testEncodeSetConnection() {

        MobicProtocolEncoder encoder = new MobicProtocolEncoder(null);
        
        Command command = new Command();
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "server.com");

        assertEquals(commandHeader + "S1IPA1=server.com*00", encoder.encodeCommand(null, command));
    }
}
