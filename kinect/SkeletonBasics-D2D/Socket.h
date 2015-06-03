#pragma once

#include "stdafx.h"
#include <strsafe.h>
#include "resource.h"
#include "NuiApi.h"
#include "Data.h"
#include <WinSock2.h>
#include <thread>
#pragma comment(lib, "ws2_32.lib")

class Socket
{
	WSADATA WSAData;
	SOCKET server;
	SOCKET connection;
	SOCKADDR_IN server_addr;
	SOCKADDR_IN connection_addr;
	FILE *print_out;
	std::thread *serverThread;

	void _accept();
	bool connected;

	UINT64 currentFrameTime;
	void _send(char* buffer);
	//void _parse();

	int _lastHand;
public:
	Socket();
	~Socket();
	void frame(boolean start);
	void pushHand(int hand_id, const Vector4 &vec);
	inline int lastHand(){ return _lastHand; }
};

