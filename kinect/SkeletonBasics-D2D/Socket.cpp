#include "Socket.h"
#include <stdio.h>
#include <stdlib.h>
#include <WinBase.h>


Socket::Socket() :
connected(false), _lastHand(-1)
{

	print_out = fopen("out.txt", "w");
	if (print_out < 0){
		exit(-1);
	}

	printf("\nInitialising Winsock...");
	if (WSAStartup(MAKEWORD(2, 2), &WSAData) != 0)
	{
		printf("Failed. Error Code : %d", WSAGetLastError());
		//return 1;
	}
	printf("Initialised.\n");

	if ((server = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET)
	{
		printf("Could not create socket : %d", WSAGetLastError());
	}

	printf("Socket created.\n");

	//Prepare the sockaddr_in structure
	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = INADDR_ANY;
	server_addr.sin_port = htons(8888);

	//Bind
	if (bind(server, (SOCKADDR*)&server_addr, sizeof(server_addr)) == SOCKET_ERROR)	{
		printf("Bind failed with error code : %d", WSAGetLastError());
	}

	printf("Bind done");

	serverThread = new std::thread(&Socket::_accept, this);

}


Socket::~Socket(){
}

void Socket::_accept(){
	listen(server, 0);

	printf("Waiting for incoming connections...");

	int connection_addr_size = sizeof(connection_addr);
	connection = accept(server, (SOCKADDR*)&connection_addr, &connection_addr_size);
	if (connection == INVALID_SOCKET) {
		printf("accept failed with error code : %d", WSAGetLastError());
	}
	else{
		send(connection, "Hello world!\r\n", 14, 0);
		u_long iMode = 1; //need pointer for one value...
		ioctlsocket(connection, FIONBIO, &iMode); //set non-blocking mode
		//closesocket(connection);
	}

	puts("Connection accepted");

	connected = true;
}


UINT64 getTime(){
	FILETIME fileTime;
	GetSystemTimeAsFileTime(&fileTime);
	UINT64 time;
	time = (UINT64)fileTime.dwLowDateTime + ((UINT64)(fileTime.dwHighDateTime) << 32LL); //a direct cast could cause aligment issues
	time /= 10000; //fileTime unit is 100ns, so 1ms=1000us=10000ns
	return time;
}

void Socket::_send(char* buffer){
	//fprintf(print_out, "%s", buffer);
	size_t size = strlen(buffer);
	fwrite(buffer, sizeof(char), size, print_out);
	if (connected) {
		int sent = send(connection, buffer, size, 0);
		if (sent != size){
			fprintf(print_out, "ERROR sent %i instead of %i\n", sent, size);
		}
	}
}

void Socket::frame(boolean start){	
	if (start) {
		currentFrameTime = getTime();
	} else {
		char buffer[128];
		sprintf_s(buffer, sizeof(buffer), "E;%I64u\n", currentFrameTime);
		_send(buffer);
	}	
}

void Socket::pushHand(int hand_id, const Vector4 &vec){
	char buffer[256];
	sprintf_s(buffer, sizeof(buffer), "%i;%f;%f;%f\n", hand_id, vec.x, vec.y, vec.z);
	_send(buffer);
};