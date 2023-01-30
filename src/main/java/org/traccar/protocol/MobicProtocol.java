/*
 * 
 */
package org.traccar.protocol;

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import org.traccar.BaseProtocol;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

public class MobicProtocol extends BaseProtocol {

    public MobicProtocol(Config config) {
        
        setSupportedDataCommands(
                Command.TYPE_CUSTOM,                
                Command.TYPE_POWER_OFF,
                Command.TYPE_REBOOT_DEVICE, 
                Command.TYPE_SET_CONNECTION);        
        
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new CharacterDelimiterFrameDecoder(1455, false, "*00"));
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new MobicProtocolEncoder(MobicProtocol.this));                
                pipeline.addLast(new MobicProtocolDecoder(MobicProtocol.this));
            }
        });
    }

}
