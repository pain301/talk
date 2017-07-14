#include "BaseSocket.h"

std::map<net_handle_t, BaseSocket*> g_socket_map;

/********************************** Manage socket handler **********************************/

/*
SOCKET => INT
net_handle_t => INT
*/
void addBaseSocket(BaseSocket * pSocket)
{
  g_socket_map.insert(make_pair((net_handle_t)pSocket->getSocket(), pSocket));
}

void removeBaseSocket(BaseSocket * pSocket)
{
  g_socket_map.erase((net_handle_t)pSocket->getSocket());
}

BaseSocket * findBaseSocket(net_handle_t fd)
{
  BaseSocket * pSocket = NULL;

  std::map<net_handle_t, BaseSocket *>::iterator iter = g_socket_map.find(fd);
  if (g_socket_map.end() != iter) {
    pSocket = iter->second;
    pSocket->addRef();
  }

  return pSocket;
}

BaseSocket::BaseSocket()
{
  m_socket = INVALID_SOCKET;
  m_state = SOCKET_STATE_IDLE;
}

BaseSocket::~BaseSocket()
{

}

void BaseSocket::setSendBufSize(uint32_t sendSize)
{
  int ret = setsockopt(m_socket, SOL_SOCKET, SO_SNDBUF, &sendSize, 4);
  if (SOCKET_ERROR == ret) {
    // log
  }

  socklen_t len = 4;
  int size = 0;
  getsockopt(m_socket, SOL_SOCKET, SO_SNDBUF, &size, &len);
}

void BaseSocket::setRecvBufSize(uint32_t recvSize)
{
  int ret = setsockopt(m_socket, SOL_SOCKET, SO_RCVBUF, &recvSize, 4);
  if (SOCKET_ERROR == ret) {
    // log
  }
  socklen_t len = 4;
  int size = 0;
  getsockopt(m_socket, SOL_SOCKET, SO_RCVBUF, &size, &len);
}

/********************************** Listen as Server **********************************/

// ERROR DEFINE
// Server listen clients
int BaseSocket::listen(const char * ip, uint16_t port,
      callback_t callback, void * data)
{
  m_localIp = ip;
  m_localPort = port;
  m_callback = callback;
  m_callbackData = data;

  m_socket = ::socket(AF_INET, SOCK_STREAM, 0);
  if (INVALID_SOCKET == m_socket) {
    // log
    return NETLIB_ERROR;
  }

  setReuseAddr(m_socket);
  setNonBlock(m_socket);

  sockaddr_in serverAddr;

  setAddr(m_localIp, m_localPort, &serverAddr);
  int ret = ::bind(m_socket, (sockaddr *)&serverAddr, sizeof(serverAddr));
  if (SOCKET_ERROR == ret) {
    // log
    ::close(m_socket);
    return NETLIB_ERROR;
  }

  ret = ::listen(m_socket, 100);
  if (SOCKET_ERROR == ret) {
    // log
    ::close(m_socket);
    return NETLIB_ERROR;
  }

  m_state = SOCKET_STATE_LISTENING;
  printf("### Server listen on ip: %s, port %d\n", m_localIp, m_localPort);

  addBaseSocket(this);
  EventDispatcher::getInstance()->addEvent(m_socket, SOCKET_READ | SOCKET_EXCEP);
  return NETLIB_OK;
}

/********************************** Connect as client **********************************/

// connect to server
int BaseSocket::connect(const char * ip, uint16_t port,
      callback_t callback, void * data)
{
  m_remoteIp = ip;
  m_remotePort = port;
  m_callback = callback;
  m_callbackData = data;

  m_socket = ::socket(AF_INET, SOCK_STREAM, 0);

  if (INVALID_SOCKET == m_socket) {
    // log
    return INVALID_SOCKET;
  }

  setNonBlock(m_socket);
  setNoDelay(m_socket);

  sockaddr_in serverAddr;
  setAddr(m_remoteIp, m_remotePort, &serverAddr);

  int ret = ::connect(m_socket, (sockaddr *)(&serverAddr), sizeof(sockaddr_in));
  if ((SOCKET_ERROR == ret) && (!isBlock(getErrorCode()))) {
    ::close(m_socket);
    return INVALID_SOCKET;
    // log
  }

  printf("### Client connect on ip: %s, port %d\n", m_remoteIp, m_remotePort);
  m_state = SOCKET_STATE_CONNECTING;

  addBaseSocket(this);
  EventDispatcher::getInstance()->addEvent(m_socket, SOCKET_ALL);
  return m_socket;
}

int BaseSocket::send(void * buf, uint32_t len)
{
  if (SOCKET_STATE_CONNECTED != m_state) {
    return NETLIB_ERROR;
  }

  int ret = ::send(m_socket, (char *)buf, len, 0);
  if (SOCKET_ERROR == ret) {
    if (isBlock(getErrorCode())) {
      ret = 0;
    } else {
      // log
    }
  }

  return ret;
}

int BaseSocket::recv(void * buf, uint32_t len)
{
  return ::recv(m_socket, (char *)buf, len, 0);
}

int BaseSocket::close()
{
  EventDispatcher::getInstance()->removeEvent(m_socket, SOCKET_ALL);
  removeBaseSocket(this);
  ::close(m_socket);
  releaseRef();

  return 0;
}

void BaseSocket::onRead()
{
  if (SOCKET_STATE_LISTENING == m_state) {
    // handle new connection
  } else {
    u_long avail = 0;
    if ((SOCKET_ERROR == ioctl(m_socket, FIONREAD, &avail) || (0 == avail))) {
      m_callback(m_callbackData, NETLIB_MSG_CLOSE, m_socket, NULL);
    } else {
      m_callback(m_callbackData, NETLIB_MSG_READ, m_socket, NULL);
    }
  }
}

void BaseSocket::onWrite()
{
  if (SOCKET_STATE_CONNECTING == m_state) {
    int error = 0;
    socklen_t len = sizeof(error);

    // CHECK
    getsockopt(m_socket, SOL_SOCKET, SO_ERROR, (void *)&error, &len);
    if (error) {
      m_callback(m_callbackData, NETLIB_MSG_CLOSE, m_socket, NULL);
    } else {
      m_state = SOCKET_STATE_CONNECTED;
      m_callback(m_callbackData, NETLIB_MSG_CONFIRM, m_socket, NULL);
    }
  } else {
    m_callback(m_callbackData, NETLIB_MSG_WRITE, m_socket, NULL);
  }
}

void BaseSocket::onClose()
{
  m_state = SOCKET_STATE_CLOSING;
  m_callback(m_callbackData, NETLIB_MSG_CLOSE, m_socket, NULL);
}

/********************************** **********************************/

int BaseSocket::getErrorCode()
{
  return errno;
}

bool BaseSocket::isBlock(int errorCode)
{
  return ((EINPROGRESS == errorCode) || (EWOULDBLOCK == errorCode));
}

void BaseSocket::setNonBlock(SOCKET socket)
{
  int ret = fcntl(socket, F_SETFL, O_NONBLOCK | fcntl(socket, F_GETFL));
  if (SOCKET_ERROR == ret) {
    // log
  }
}

void BaseSocket::setReuseAddr(SOCKET socket)
{
  int reuse = 1;

  // reuse addr and port when server restart
  int ret = setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, (char *)&reuse, sizeof(reuse));
  if (SOCKET_ERROR == ret) {
    // log
  }
}

void BaseSocket::setNoDelay(SOCKET socket)
{
  int nodelay = 1;

  // Disabled Nagle’s Algorithm
  int ret = setsockopt(socket, IPPROTO_TCP, TCP_NODELAY, (char *)&nodelay, sizeof(nodelay));
  if (SOCKET_ERROR == ret) {
    // log
  }
}

void BaseSocket::setAddr(const char * ip, const uint16_t port, sockaddr_in * pAddr)
{
  memset(pAddr, 0, sizeof(sockaddr_in));

  // man 7 ip
  pAddr->sin_family = AF_INET;
  pAddr->sin_port = htons(port);
  pAddr->sin_addr.s_addr = inet_addr(ip);

  if (INADDR_NONE == pAddr->sin_addr.s_addr) {
    hostent * host = gethostbyname(ip);
    if (NULL == host) {
      // log
      return ;
    }
    pAddr->sin_addr.s_addr = *(uint32_t *)host->h_addr;
  }
}

void BaseSocket::acceptNewSocket()
{
  SOCKET fd = 0;
  sockaddr_in peer_addr;
  socklen_t addr_len = sizeof(sockaddr_in);

  char ip_str[64] = {0};

  while (INVALID_SOCKET != (fd = ::accept(m_socket, (sockaddr *)&peer_addr, &addr_len))) {
    BaseSocket * pSocket = new BaseSocket();
    uint32_t ip = ntohl(peer_addr.sin_addr.s_addr);
    uint16_t port = ntohs(peer_addr.sin_port);

    snprintf(ip_str, sizeof(ip_str), "%d.%d.%d.%d", ip >> 24, (ip >> 16) & 0xFF, (ip >> 8) & 0xFF, ip & 0xFF);

    // log
    pSocket->setSocket(fd);
    pSocket->setCallback(m_callback);
    pSocket->setCallbackData(m_callback_data);
    pSocket->setState();
    pSocket->setRemoteIp(ip_str);
    pSocket->setRemotePort(port);

    setNoDelay(fd);
    setNonblock(fd);
    addBaseSocket(pSocket);
    EventDispatch::Instance()->AddEvent(fd, SOCKET_READ | SOCKET_EXCEP);
    m_callback(m_callback_data, NETLIB_MSG_CONNECT, (net_handle_t)fd, NULL);
  }
}
