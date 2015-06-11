import socket
import re
import asyncio
import math
from statistics import variance

# f = open('test2.txt', 'w')
kinect_read_buffer = ""

regex_android = re.compile("^b'(?P<time>[0-9]{13}),ACCEL,(?P<x>-?[0-9]+.[0-9]+),(?P<y>-?[0-9]+.[0-9]+),(?P<z>-?[0-9]+.[0-9]+)\\\\n'$")
regex_kinect = re.compile("(?P<number>[0-9]+);(?P<x>-?[0-9]+.[0-9]+);(?P<y>-?[0-9]+.[0-9]+);(?P<z>-?[0-9]+.[0-9]+)")
regex_kinect_time = re.compile("^b'.+E;(?P<time>[0-9]{13})\\\\n'$")
kinect_pos = []
kinect_acc = []
android_acc = []
merged_data = []

def mergeData():
    if len(kinect_acc) > 0:
        kinect = kinect_acc[0]
        found_match = False
        found_index = -1
        for i, android in enumerate(android_acc):
            if android[0] > kinect[0]:
                found_index = i
                break
        if found_index != -1:
            merged_data.append((android_acc[found_index], kinect_acc[0])) # MAYBE: mean android_acc
            del android_acc[:found_index+1]
            del kinect_acc[0]
            removeOldNumbers()
            mergeData()

def removeOldNumbers():
    last_data = merged_data[-1]
    kinect = last_data[1]
    kinect_hand = kinect[1]
    valid_numbers = kinect_hand.keys()

    for data in merged_data:
        kinect_dict = data[1][1]
        key_to_remove = []
        for key, value in kinect_dict.items():
            if key not in valid_numbers:
                key_to_remove.append(key)
        for key in key_to_remove:
            try:
                del kinect_dict[key]
            except Exception:
                pass

def choose():
    diff_dict = {}
    for data in merged_data:
        acc_android = data[0][1]
        acc_kinect_dict = data[1][1]
        s_acc_android = math.sqrt(acc_android.x*acc_android.x+acc_android.y*acc_android.y+acc_android.z*acc_android.z)
        for key, value in acc_kinect_dict.items():
            if key not in diff_dict:
                diff_dict[key] = []
            s_acc_kinect = math.sqrt(value.x*value.x+value.y*value.y+value.z*value.z)
            ratio = (s_acc_android-s_acc_kinect)/s_acc_android
            diff_dict[key].append(ratio)
            # print("key")
            # print(key)
            # print(s_acc_kinect)

    var_dict = {}
    for key, value in diff_dict.items():
        if len(value) > 2:
            # print(key)
            # print(value)
            var_dict[key] = variance(value)
    if var_dict:
        best_key = min(var_dict, key=var_dict.get)
        print("Choose ", best_key)
        return best_key
    else:
        print("Choose ", -1)
        return -1

def testAndroid():
    server_android = ServerAndroid()
    # f = open('test.txt')
    # raw_string = f.readline()
    # while raw_string is not '':
    #     server_android.data_received(raw_string)
    #     raw_string = f.readline()
    b = b'12345678900000,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900100,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900200,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900300,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900400,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900500,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900600,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'12345678900700,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)

def testKinect():
    client_kinect = ClientKinect()
    a = b"1;1.1;1.1;1.1\nE;12345678900000\n"
    client_kinect.data_received(a)
    a = b"1;10.2;1.1;1.1\nE;12345678900100\n"
    client_kinect.data_received(a)
    a = b"1;10.3;1.1;1.1\nE;12345678900200\n"
    client_kinect.data_received(a)
    a = b"1;1.4;1.1;1.1\n2;0.0;0.0;0.0\nE;12345678900300\n"
    client_kinect.data_received(a)
    a = b"1;1.5;1.1;1.1\n2;1.0;1.0;1.0\nE;12345678900400\n"
    client_kinect.data_received(a)
    a = b"1;10.6;1.1;1.1\n2;3.0;3.0;3.0\nE;12345678900500\n"
    client_kinect.data_received(a)
    a = b"1;10.7;1.1;1.1\n2;6.0;6.0;6.0\nE;12345678900600\n"
    client_kinect.data_received(a)
    a = b"1;1.8;1.1;1.1\n2;10.0;10.0;10.0\nE;12345678900700\n"
    client_kinect.data_received(a)

class Coord:
    def __init__(self, x, y, z):
        self.x = float(x)
        self.y = float(y)
        self.z = float(z)

class MI12:
    def __init__(self):
        loop = asyncio.get_event_loop()
        server_android = loop.create_server(ServerAndroid, '172.25.42.58', 11337)
        client_kinect = loop.create_connection(ClientKinect, '172.25.13.82', 8888)
        loop.run_until_complete(server_android)
        loop.run_until_complete(client_kinect)
        # testAndroid()
        # testKinect()
        loop.run_forever()

class ClientKinect(asyncio.Protocol):
    def connection_made(self, transport):
        print('Kinect connected')
        self.transport = transport

    def connection_lost(self, exc):
        print('Kinect disconnected')

    def data_received(self, raw_data):
        # print('Kinect says: ', str(raw_data))
        global kinect_read_buffer
        kinect_read_buffer += str(raw_data)
        splited = str(kinect_read_buffer).split('\n')
        for data in splited:
            kinect_read_buffer = ""
            extracted_data = self.extractData(str(data))
            if extracted_data is not None:
                print(str(data))
                kinect_pos.append(extracted_data)
                if len(kinect_pos) == 3:
                    acc = self.compute_acc(kinect_pos[0], kinect_pos[1], kinect_pos[2])
                    kinect_acc.append(acc)
                    del kinect_pos[0]
                    mergeData()
                    choosen = choose()
                    self.transport.write((str(choosen)+str('\n')).encode())
            else:
                kinect_read_buffer = data
                # print("Wrong kinect data")

    def extractData(self, raw_input):
        time_group = regex_kinect_time.match(raw_input)
        coord_match = regex_kinect.finditer(raw_input)
        if coord_match and time_group:
            time = float(time_group.group('time'))/1000
            coords = {}
            for coord in coord_match:
                coords[float(coord.group('number'))] = Coord(coord.group('x'), coord.group('y'), coord.group('z'))
            return (time, coords)
        else:
            return None

    def compute_acc(self, p1, p2, p3):
        v1 = self.my_sub(p1, p2)
        v2 = self.my_sub(p2, p3)
        return self.my_sub(v1, v2)

    def my_sub(self, data1, data2):
        time1 = data1[0]
        time2 = data2[0]
        delta = time2-time1
        coords1 = data1[1]
        coords2 = data2[1]
        coords = {}
        for number, coord1 in coords1.items():
            if number in coords2:
                coord2 = coords2[number]
                coords[number] = Coord((coord2.x-coord1.x)/delta, (coord2.y-coord1.y)/delta, (coord2.z-coord1.z)/delta)
                # print("number")
                # print(number)
                # print((coord2.x-coord1.x))
        return ((time2+time1)/2, coords)

class ServerAndroid(asyncio.Protocol):
    def connection_made(self, transport):
        peername = transport.get_extra_info('peername')
        print('Android connected from {}'.format(peername))
        self.transport = transport

    def connection_lost(self, exc):
        print('Android disconnected')

    def data_received(self, data):
        print('Android says: ', data)
        extracted_data = self.extractData(str(data))
        if extracted_data is not None:
            android_acc.append(extracted_data)

    def extractData(self, raw_input):
        data = regex_android.match(raw_input)
        if data:
            return (float(data.group('time'))/1000, Coord(data.group('x'), data.group('y'), data.group('z')))
        else:
            return None

if __name__ == "__main__":
    MI12()


# @asyncio.coroutine
# def readKinect(self):
#   s = socket.socket()
#   address = '127.0.0.1'
#   port = 80
#   try:
#       s.connect((address, port)) 
#   except Exception as e:
#       print('something\'s wrong with %s:%d. Exception type is %s' % (address, port, e))

    
#   while True:
#       raw_input = s.recv(1024)
#       if raw_input:
#           print("Kinect said: "+str(raw_input))
#       else:
#           print("Kinect sucks...")

# @asyncio.coroutine
#   def readAndroid(self):
#       PORT = 11337
#       s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#       s.bind(('', PORT))
#       s.listen(5)
#       print("Wait for Android ...")
#       conn, addr = s.accept()
#       while True:
#           t = conn.recv(1024)
#           raw_input = str(t)
#           if not raw_input:
#               break
#           data = self.extractData(raw_input)
#           if data:
#               print("Android said: "+str(data))
#               self.android_FIFO.append(data)
#           else:
#               print("Android sucks..."+str(raw_input))
            
#       conn.close()