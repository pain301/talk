#include "netlib.h"
#include "BaseSocket.h"
#include "EventDispatcher.h"

int netlib_init()
{
  int ret = NETLIB_OK;
  return ret;
}

int netlib_destroy()
{
  int ret = NETLIB_OK;
  return ret;
}

int netlib_listen(const char * ip, uint16_t port, callback_t callback, void * data)
{
  BaseSocket * pSocket = new BaseSocket();
  if (NULL == pSocket) {
    return NETLIB_ERROR;
  }

  int ret = pSocket->listen(ip, port, callback, data);

  if (NETLIB_ERROR == ret) {
    delete pSocket;
  }

  return ret;
}

net_handle_t netlib_connect(const char * ip, uint16_t port, callback_t callback, void * data)
{
  BaseSocket * pSocket = new BaseSocket();
  if (NULL == pSocket) {
    return NETLIB_ERROR;
  }

  net_handle_t handle = pSocket->connect(ip, port, callback, data);

  if (NETLIB_INVALID_HANDLE == handle) {
    delete pSocket;
  }

  return handle;
}

int netlib_send(net_handle_t handle, void * buf, int len)
{
  BaseSocket * pSocket = findBaseSocket(handle);

  if (NULL == pSocket) {
    return NETLIB_ERROR;
  }

  int ret = pSocket->send(buf, len);
  pSocket->releaseRef();

  return ret;
}

int netlib_recv(net_handle_t handle, void * buf, int len)
{
  BaseSocket * pSocket = findBaseSocket(handle);

  if (NULL == pSocket) {
    return NETLIB_ERROR;
  }

  int ret = pSocket->recv(buf, len);
  pSocket->releaseRef();

  return ret;
}

int netlib_close(net_handle_t handle)
{
  BaseSocket * pSocket = findBaseSocket(handle);

  if (NULL == pSocket) {
    return NETLIB_ERROR;
  }

  int ret = pSocket->close();
  pSocket->releaseRef();

  return ret;
}

int netlib_option(net_handle_t handle, int opt, void * optval)
{
  BaseSocket * pSocket = findBaseSocket(handle);

  if (NULL == pSocket) {
    return NETLIB_ERROR;
  }

  if ((NETLIB_OPT_GET_REMOTE_IP <= opt) && (NULL == optval)) {
    return NETLIB_ERROR;
  }

  switch (opt) {
  case NETLIB_OPT_SET_CALLBACK:
    pSocket->setCallback((callback_t)optval);
    break;
  case NETLIB_OPT_SET_CALLBACK_DATA:
    pSocket->setCallbackData(optval);
    break;
  case NETLIB_OPT_GET_REMOTE_IP:
    *(string *)optval = pSocket->getRemoteIp();
    break;
  case NETLIB_OPT_GET_REMOTE_PORT:
    *(uint16_t *)optval = pSocket->getRemotePort();
    break;
  case NETLIB_OPT_GET_LOCAL_IP:
    *(string *)optval = pSocket->getLocalIp();
    break;
  case NETLIB_OPT_GET_LOCAL_PORT:
    *(uint16_t *)optval = pSocket->getLocalPort();
    break;
  case NETLIB_OPT_SET_SEND_BUF_SIZE:
    pSocket->setSendBufSize(*(uint32_t *)optval);
    break;
  case NETLIB_OPT_SET_RECV_BUF_SIZE:
    pSocket->setRecvBufSize(*(uint32_t *)optval);
    break;
  }

  pSocket->releaseRef();
  return NETLIB_OK;
}

int netlib_register_timer(callback_t callback, void * data, uint64_t interval)
{
  EventDispatcher::getInstance()->addTimer(callback, data, interval);

  return 0;
}

int netlib_delete_timer(callback_t callback, void * data)
{
  EventDispatcher::getInstance()->removeTimer(callback, data);

  return 0;
}

int netlib_add_loop(callback_t callback, void * data)
{
  EventDispatcher::getInstance()->addLoop(callback, data);

  return 0;
}

void netlib_eventloop(uint32_t wait_timeout)
{
  EventDispatcher::getInstance()->startDispatch(wait_timeout);
}

void netlib_stop_event()
{
  EventDispatcher::getInstance()->stopDispatch();
}

bool netlib_is_running()
{
  return EventDispatcher::getInstance()->isRunning();
}
