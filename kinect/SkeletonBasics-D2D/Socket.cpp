#include "Socket.h"
#include <stdio.h>
#include <stdlib.h>
#include <WinBase.h>

#define ARRAY_SIZE(x) (sizeof(x)/sizeof(x[0]))


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
		fprintf(print_out, "ERROR accept failed with error code : %d", WSAGetLastError());
	}
	else{
		send(connection, "Hello world!\r\n", 14, 0);
		u_long iMode = 1; //need pointer for one value...
		ioctlsocket(connection, FIONBIO, &iMode); //set non-blocking mode
		//closesocket(connection);
	}

	fprintf(print_out, "Connection accepted");

	connected = true;
}





char* myStrrchr(char* str, char* end, char delim){
	while (end >= str){
		if (*end == delim){
			return end;
		}
		end--;
	}
	return NULL;
}

int Socket::_parseString(const char* in){


	FILE    *printOut = print_out,
			*printErr = print_out,
			*printIn = print_out;

	size_t inSize = strlen(in); //don't take \0
	fprintf(printOut, "in[%i]:{%s}\n", inSize, in);

	//	tried to use std stream, but they don't support non-blocking mode
	//	and they're not thread safe
	//	don't even think about using boost again ^^
	//	so poor's man stream it is...
	//	( not even circular BTW :D )

	static char stream[1024] = { '\0' };
	static char* streamPos = stream;
	static boolean discarding = false; //if there'll be data loss, wait for next valid frame

	if (inSize > ARRAY_SIZE(stream) - 1){
		in += ((int)inSize - (ARRAY_SIZE(stream) - 1));
		inSize = ARRAY_SIZE(stream) - 1;
		fprintf(printErr, "WARN in too long, only taking last %i:{%s}\n", ARRAY_SIZE(stream) - 1, in);
		discarding = true;
	}

	if (streamPos + inSize > stream + ARRAY_SIZE(stream) - 1){ //room for \0
		fprintf(printErr, "cannot add to buffer: streamPos=%i + inSize=%i => sizeof(stream)-1=%i\n", (streamPos - stream) / sizeof(char), inSize, sizeof(stream) / sizeof(char)-1);
		fprintf(printErr, "Discarding:{%s}\n", stream);
		streamPos = stream;
		stream[0] = '\0';
		discarding = true;
	}
	strcat(streamPos, in);
	streamPos += inSize;
	fprintf(printErr, "Stream:{%s}\n", stream);


	char* lastPos = myStrrchr(stream, streamPos, '\n');
	if (lastPos != NULL){
		char* beforeLastPos = myStrrchr(stream, lastPos - 1, '\n'); // -1 to not find the same //Will match on "\n\n"... enough of parsing :(
		if (beforeLastPos != NULL) discarding = false;
		if (!discarding){
			if (beforeLastPos == NULL) beforeLastPos = stream - 1;
			fprintf(printErr, "beforeLastPos=%i lastPos=%i\n", (beforeLastPos - stream) / sizeof(char), (lastPos - stream) / sizeof(char));
			size_t idSize = lastPos - beforeLastPos - 1;
			//idSize might be 0
			fprintf(printErr, "found idSize=%i\n", idSize);
			char idString[128];
			if (idSize <= ARRAY_SIZE(idString) - 1 && idSize > 0){ //room for \0
				memcpy(idString, beforeLastPos + 1, idSize);
				idString[idSize] = '\0';
				fprintf(printErr, "idString={%s}\n", idString);
				char* end;
				int id = strtod(idString, &end);
				if (end != idString){ //something found
					fprintf(printErr, "HAND_ID=%i\n", id);
					return id;
				}
				else {
					fprintf(printErr, "ERROR strtod didn't found anything in '%s'\n", idString);
				}

			} //will be cleaned on buffer overflow protection
		}
	}
	return -1;
}

void Socket::_parse(){
	if (!connected) return;
	char buffer[512];
	int written = recv(connection, buffer, sizeof(buffer)-1, 0);
	if (written == SOCKET_ERROR){
		if (WSAGetLastError() == WSAEWOULDBLOCK){
			fprintf(print_out, "recv nothing:\n");
		}
		else {
			fprintf(print_out, "ERROR recv n:%i\n", WSAGetLastError());
		}
		return;
	}
	buffer[written] = '\0';

	fprintf(print_out, "recv:%s\n", buffer);

	_lastHand = _parseString(buffer);
	fprintf(print_out, "HAND FOUND:%i\n", _lastHand);


}


UINT64 getTime(){
	FILETIME fileTime;
	GetSystemTimeAsFileTime(&fileTime);
	UINT64 time;
	time = (UINT64)fileTime.dwLowDateTime + ((UINT64)(fileTime.dwHighDateTime) << 32LL); //a direct cast could cause aligment issues
	time -= 116444736000000000LL; //convert to Unix Epoch
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
		_parse();
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