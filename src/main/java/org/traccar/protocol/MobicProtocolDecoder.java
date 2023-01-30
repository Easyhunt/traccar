/*
 * 
 */
package org.traccar.protocol;

import io.netty.channel.Channel;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MobicProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobicProtocolDecoder.class);

    public static final String DEFAULT_SEQUENCE_ID = "00001";
    
    private static final String HEADER_QGP2 = "$QGP2000";
    private static final String HEADER_QEVT = "$QEVT000";
    private static final String HEADER_QTA3 = "$QTA3000";
    private static final String END_MESSAGE = "*00";
    private final SimpleDateFormat timePositionSDF = new SimpleDateFormat("ddMMyyHHmmss");
    
    private String sequenceId = DEFAULT_SEQUENCE_ID;
    
    public MobicProtocolDecoder(Protocol protocol) {
        super(protocol);
    }
    
    public String getSequenceId() {
        return sequenceId;
    }
    
    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        sentence = sentence.replace("\r", "").replace("\n", "");

        LOGGER.info(sentence);

        int sentenceLength = sentence.length();        
        
        if ((!sentence.startsWith(HEADER_QGP2) && !sentence.startsWith(HEADER_QEVT) && !sentence.startsWith(HEADER_QTA3)) 
                || sentence.indexOf(END_MESSAGE) + 3 != sentenceLength || sentenceLength < 111 || sentenceLength > 1455) {

            LOGGER.info("sentence.length() = {}; sentence.indexOf(END_MESSAGE)={}; HEADER_QGP2={}; HEADER_QEVT={}; HEADER_QTA3={}; Check_sentence={}", 
                    sentenceLength, sentence.indexOf(END_MESSAGE), sentence.startsWith(HEADER_QGP2), 
                    sentence.startsWith(HEADER_QEVT), sentence.startsWith(HEADER_QTA3),
                    ((!sentence.startsWith(HEADER_QGP2) && !sentence.startsWith(HEADER_QEVT) && !sentence.startsWith(HEADER_QTA3)) 
                            || sentence.indexOf(END_MESSAGE) + 3 != sentenceLength || sentenceLength < 111 || sentenceLength > 1455));
            return null;
        }
        
        String serialNo = sentence.substring(8, 20);
        sequenceId = sentence.substring(20, 25);
        
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, serialNo);
        if (deviceSession == null) {
            LOGGER.warn("The deviceSession is null for serialNo={} and packetId={}", serialNo, sequenceId);
            return null;
        }

        List<Position> positions = null;
        
        if (sentence.startsWith(HEADER_QGP2)) {

            positions = new LinkedList<>();
            
            sentence = sentence.replace('_', '0').replace(' ', '0');
            
            Double batteryDbl = null;
            String batteryStr = sentence.substring(25, 28);
            if (StringUtils.isNumeric(batteryStr)) {
                batteryDbl = Double.valueOf(batteryStr) / 100;
                if (batteryDbl > 0) {
                    if (batteryDbl < 3.451) {
                        batteryDbl = 3.451;
                    } else {
                        if (batteryDbl > 4.12) {
                            batteryDbl = 4.12;
                        }
                    }
                }

                batteryDbl = (1.747 * Math.pow(batteryDbl, 2) - 11.744 * batteryDbl + 19.728) * 100;
            }
            Long battery = batteryDbl == null ? 0L : Math.round(batteryDbl);
            
            String rssiStr = sentence.substring(33, 35);
            if (StringUtils.isNumeric(rssiStr)) {
                if (Integer.valueOf(rssiStr) > 31) {
                    rssiStr = "0";
                }
            } else {
                rssiStr = "0";
            }
            Integer rssi = Integer.valueOf(rssiStr);
            
            String numItemsStr = sentence.substring(42, 44);
            int numItems = StringUtils.isNumeric(numItemsStr) ? Integer.valueOf(numItemsStr) : 0;
            String itemsStr = sentence.substring(44, sentence.indexOf(END_MESSAGE));
            List<String> items = Arrays.asList(itemsStr.split("[|]"));

            if (items.size() != numItems) {
                LOGGER.warn("Expected numItems = {}; Actual numItems = {};",  numItems, items.size());
            }
            
            for (String item : items) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());
                position.setValid(true);
                position.set(Position.KEY_BATTERY_LEVEL, battery.intValue());
                position.set(Position.KEY_RSSI, rssi);
                position.setTime(timePositionSDF.parse(item.substring(4, 16)));
                
                if (StringUtils.isNumeric(item.substring(16, 24))) {
                    double sign = item.substring(24, 25).equals("S") ? -1.00 : 1.00;
                    double coordinate = Double.parseDouble(item.substring(16, 18));
                    if (Integer.valueOf(item.substring(18, 20)) < 60) {
                        coordinate += Double.parseDouble(item.substring(18, 20) + "." + item.substring(20, 24)) / 60;
                    }
                    position.setLatitude(sign * coordinate);    
                }

                if (StringUtils.isNumeric(item.substring(25, 34))) {
                    double sign = item.substring(34, 35).equals("W") ? -1.00 : 1.00;
                    double coordinate = Double.parseDouble(item.substring(25, 28));
                    if (Integer.valueOf(item.substring(28, 30)) < 60) {
                        coordinate += Double.parseDouble(item.substring(28, 30) + "." + item.substring(30, 34)) / 60;
                    }
                    position.setLongitude(sign * coordinate);    
                }

                position.setAltitude(Double.parseDouble(item.substring(35, 40)));
                position.setCourse(Double.parseDouble(item.substring(40, 45)));
                position.setSpeed(convertSpeed(Double.parseDouble(item.substring(45, 51)), "kn"));
                
                if (StringUtils.isNumeric(item.substring(58, 59))) {
                    position.set(Position.KEY_SATELLITES_VISIBLE, Integer.parseInt(item.substring(58, 59)));
                } else {
                    position.set(Position.KEY_SATELLITES_VISIBLE, 4);
                }

                if (StringUtils.isNumeric(item.substring(59, 63).replace('.', '0'))) {
                    position.set(Position.KEY_HDOP, Double.parseDouble(item.substring(59, 63)));
                } else {
                    position.set(Position.KEY_HDOP, 10.0);
                }

                positions.add(position);                
            }
            
        } else if (sentence.startsWith(HEADER_QEVT)) {
            Device device = getCacheManager().getObject(Device.class, deviceSession.getDeviceId());
            if (device != null) {
                device.set("IMEI", sentence.substring(25, 42));                
                device.set("ICCID", sentence.substring(42, 61));
                device.set("hardwareVersion", sentence.substring(61, 73).replace("_", ""));                
                device.set("firmwareVersion", sentence.substring(73, 93).replace("_", ""));
                
                boolean isUpdateConfig = false;  
                String val = device.getString("BARKEN");
                if (val == null || !val.equals("OFF")) {
                    getCommandsManager().sendCommand(createCustomCommand(device.getId(), "BARKEN=OFF"));
                    device.set("BARKEN", "OFF");
                    isUpdateConfig = true;
                }

                val = device.getString("NETRAT");
                if (val == null || !val.equals("0")) {
                    getCommandsManager().sendCommand(createCustomCommand(device.getId(), "NETRAT=0"));
                    device.set("NETRAT", "0");
                    isUpdateConfig = true;                    
                }

                val = device.getString("TMRWUE");
                if (val == null || !val.equals("TRUE")) {
                    getCommandsManager().sendCommand(createCustomCommand(device.getId(), "TMRWUE=TRUE"));
                    device.set("TMRWUE", "TRUE");
                    isUpdateConfig = true;                    
                }
                
                if (isUpdateConfig) {
                    getCommandsManager().sendCommand(createCustomCommand(device.getId(), "RESETU=EXECUTE"));
                }
                
                getCacheManager().updateDevice(true, device);
            }
        } else if (sentence.startsWith(HEADER_QTA3)) {            
            //Message which is the detailed log from the accelerometer. 
            //Skip now. 
        }
        
        if (channel != null && StringUtils.isNoneBlank(sequenceId) && !sequenceId.equals(DEFAULT_SEQUENCE_ID)) {
            channel.writeAndFlush(new NetworkMessage("$AACK00000" + sequenceId + "*00", remoteAddress));
        }
        
        return positions;
    }
    
    private Command createCustomCommand(Long deviceId, String customCommand) {
        Command command = new Command();
        command.setId(0);
        command.setDeviceId(deviceId);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, customCommand);
        command.setDescription(customCommand);
        return command;
    }
}
