#include "EventDispatcher.h"

#define MAX_FD_SIZE 1024

static bool running = false;
static int epfd = -1;

bool is_running()
{
  return running;
}

void event_dispatcher_initialize()
{
  epfd = epoll_create(MAX_FD_SIZE);
  if (-1 == epfd) {
    // TODO
  }
}

void event_dispatcher_finalize()
{
  if (0 < m_epfd)
    close(m_epfd);
}

void start_event_dispatch(uint32_t wait_timeout)
{
  if (running) {
    // TODO
    return ;
  }

  struct epoll_event events[MAX_FD_SIZE];
  int nfds = 0;
  while (running) {
    nfds = epoll_wait(epfd, events, MAX_FD_SIZE, wait_timeout);
    if (-1 == nfds) {
      // TODO
      break;
    }

    for (int i = 0; i < nfds; ++i) {
      int fd = events[i].data.fd;

      // find socket by fd, check if exists
      // read, write, close by pSocket
      #ifdef EPOLLRDHUP
      if (events[i].events & EPOLLRDHUP) {
        // TODO onclose?
      }
      #endif
      if (events[i].events & EPOLLIN) {
        // TODO read
      }
      if (events[i].events & EPOLLOUT) {
        // TODO write
      }
      if (events[i].events & (EPOLLPRI | EPOLLERR | EPOLLHUP)) {
        // TODO close?
      }
      // socket ptr release ?
    }
    //checkTimer();checkLoop();
  }
}

void stop_event_dispatch()
{
  running = false;
}

void add_event(int fd, uint8_t event)
{

}

void remove_event(int fd, uint8_t event)
{

}

void add_timer(callback_t callback, void * user_data, uint64_t interval)
{

}

void remove_timer(callback_t callback, void * user_data)
{

}

void add_loop(callback_t callback, void * user_data)
{

}

void EventDispatcher::addEvent(SOCKET fd, uint8_t event)
{
  struct epoll_event ev;
  ev.data.fd = fd;

  // Edge Trigger
  ev.events = EPOLLIN | EPOLLOUT | EPOLLET | EPOLLPRI | EPOLLERR | EPOLLHUP;
  if (0 != epoll_ctl(m_epfd, EPOLL_CTL_ADD, fd, &ev)) {
    // error log
  }
}

void EventDispatcher::removeEvent(SOCKET fd, uint8_t event)
{
  if (0 != epoll_ctl(m_epfd, EPOLL_CTL_DEL, fd, NULL)) {
    // error log
  }
}

void EventDispatcher::addTimer(callback_t callback, void * user_data, uint64_t interval)
{
  list<TimerItem *>::iterator iter;
  for (iter = m_timerList.begin(); iter != m_timerList.end(); ++iter) {
    TimerItem * ti = *iter;
    if (ti->callback == callback && ti->user_data == user_data) {
      ti->interval = interval;
      ti->next_tick = get_tick_count() + interval;
      return ;
    }
  }

  TimerItem * ti = new TimerItem;
  ti->callback = callback;
  ti->user_data = user_data;
  ti->interval = interval;
  ti->next_tick = get_tick_count() + interval;
  m_timerList.push_back(ti);
}

void EventDispatcher::removeTimer(callback_t callback, void * user_data)
{
  list<TimerItem *>::iterator iter;
  for (iter = m_timerList.begin(); iter != m_timerList.end(); ++iter) {
    TimerItem * ti = iter;
    if (ti->callback == callback && ti->user_data == user_data) {
      m_itemList.erase(iter);
      delete ti;
      ti = NULL;
      return ;
    }
  }
}

void EventDispatcher::addLoop(callback_t callback, void * user_data)
{
  TimerItem * ti = new TimerItem;
  ti->callback = callback;
  ti->user_data = user_data;
  m_loopList.push_back(ti);
}

void EventDispatcher::checkTimer()
{
  list<TimerItem>::iterator iter;

  // FIXME
  uint64_t cur_tick = get_tick_count();

  for (iter = m_timerList.begin(); iter != m_timerList.end(); ++iter) {
    TimerItem * ti = *iter;
    if (ti->next_tick <= cur_tick) {
      ti->next_tick += ti->interval;
      ti->callback(ti->user_data, NETLIB_MSG_TIMER, 0, NULL);
    }
  }
}

void EventDispatcher::checkLoop()
{
  list<TimerItem>::iterator iter;
  for (iter = m_timerList.begin(); iter != m_timerList.end(); ++iter) {
    TimerItem * ti = *iter;
    ti->callback(ti->user_data, NETLIB_MSG_LOOP, 0, NULL);
  }
}


