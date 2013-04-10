package varick

import java.util.UUID

final class StubTCPConnection extends TCPConnection(UUID.randomUUID(),null,null,1,1){
  override def read() = Some(Array())
}
