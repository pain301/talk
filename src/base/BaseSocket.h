#ifndef __BASE_SOCKET_H__
#define __BASE_SOCKET_H__

#include "types.h"
#include "util.h"

enum
{
  SOCKET_STATE_IDLE,
  SOCKET_STATE_LISTENING,
  SOCKET_STATE_CONNECTING,
  SOCKET_STATE_CONNECTED,
  SOCKET_STATE_CLOSING
};

class BaseSocket : public CRefObject
{
public:
  BaseSocket();
  virtual ~BaseSocket();

  void setSocket(SOCKET socket) { m_socket = socket; }
  SOCKET getSocket() { return m_socket; }
  void setRemotePort(uint16_t remotePort) { m_remotePort = remotePort; }
  uint16_t getRemotePort() { return m_remotePort; }
  void setRemoteIp(const char * ip) { m_remoteIp = ip; }
  const char * getRemoteIp() { return m_remoteIp.c_str(); }
  uint16_t getLocalPort() { return m_localPort; }
  const char * getLocalIp() { return m_localIp.c_str(); }
  void setState(uint8_t state) { m_state = state; }
  uint8_t getState() { return m_state; }
  void setCallback(callback_t callback) { m_callback = callback; }
  void setCallbackData(void * callback_data) { m_callbackData = callback_data; }

  void setSendBufSize(uint32_t sendSize);
  void setRecvBufSize(uint32_t recvSize);

  int listen(const char * ip, uint16_t port, callback_t callback, void * data);
  int connect(const char * ip, uint16_t port, callback_t callback, void * data);
  int send(void * buf, uint32_t len);
  int recv(void * buf, uint32_t len);
  int close();

  void onRead();
  void onWrite();
  void onClose();
private:
  SOCKET m_socket;
  uint16_t m_localPort;
  string m_localIp;
  uint16_t m_remotePort;
  string m_remoteIp;
  uint8_t m_state;

  callback_t m_callback;
  void * m_callbackData;

  int getErrorCode();
  bool isBlock(int errorCode);
  void setNonBlock(SOCKET socket);
  void setReuseAddr(SOCKET socket);
  void setNoDelay(SOCKET socket);
  void setAddr(const char * ip, const uint16_t port, sockaddr_in * pAddr);
  void acceptNewSocket();
}

BaseSocket * findBaseSocket(SOCKET socket);

#endif
