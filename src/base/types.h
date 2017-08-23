#ifndef __TYPES_H__
#define __TYPES_H__

#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <pthread.h>

#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netdb.h>

#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>
#include <signal.h>

#if 0
#define closesocket close
#define ioctlsocket ioctl
#endif

typedef unsigned char uchar_t;

#if 0
typedef int net_handle_t;
typedef int conn_handle_t;
#endif

const uint32_t INVALID_UINT32  = (uint32_t) -1;
const uint32_t INVALID_VALUE = 0;

const int NETLIB_INVALID_HANDLE = -1;
const int SOCKET_ERROR   = -1;
const int INVALID_SOCKET = -1;

enum {
  NETLIB_OK     = 0,
  NETLIB_ERROR  = -1
};

enum
{
  NETLIB_MSG_CONNECT = 1,
  NETLIB_MSG_CONFIRM,
  NETLIB_MSG_READ,
  NETLIB_MSG_WRITE,
  NETLIB_MSG_CLOSE,
  NETLIB_MSG_TIMER,
  NETLIB_MSG_LOOP
};

typedef void (*callback_t)(void * callback_data, uint8_t msg, uint32_t handle, void * param);

#endif
