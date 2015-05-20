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
public:
	Socket();
	~Socket();
	void pushHand(char side, int skel_id, const Vector4 &vec);

};

