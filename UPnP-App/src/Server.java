import org.teleal.cling.binding.annotations.*;
import org.teleal.common.util.ByteArray;
import org.teleal.common.util.Base64Coder;
import java.util.Random;
import java.util.zip.CRC32;

//TODO: add options that can be set on the server by the client to demo
//      transfers in both directions

@UpnpService(
  serviceId = @UpnpServiceId(value = "LaunchALPAppServer",
                             namespace = "Server"),
  serviceType = @UpnpServiceType(value = "LaunchALPApp",
                                 namespace = "schemas-Server",
                                 version = 1)
)
public class Server {
  
  @UpnpStateVariable(sendEvents = false)
  private String data;
  
  @UpnpStateVariable(sendEvents = false)
  private Integer checksum;
  
  public Server() {
    byte[] rawData = new byte[1024*1024]; //one MB of data
    new Random().nextBytes(rawData);
    data = new String(Base64Coder.encode(rawData));
    checksum = calculateChecksum(rawData); //Autoboxing!
  }
  
  
  @UpnpAction(out = @UpnpOutputArgument(name = "DataChecksum"))
  public Integer getChecksum() {
    return checksum;
  }
  
  @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
  public String getData() {
    return data;
  }
  
  public static int calculateChecksum(Byte[] data) {
    return calculateChecksum(ByteArray.toPrimitive(data));
  }
  
  public static int calculateChecksum(byte[] data) {
    CRC32 crc = new CRC32();
    crc.update(data);
    return (int)crc.getValue();
  }
}
