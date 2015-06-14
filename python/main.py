import socket
import re
import asyncio
import math
import numpy
import csv

# from statistics import variance
# import scipy

# f = open('test2.txt', 'w')
kinect_read_buffer = ""
android_read_buffer = ""

regex_android = re.compile("(?P<time>[0-9]{13}),ACCEL,(?P<x>-?[0-9]+.[0-9]+),(?P<y>-?[0-9]+.[0-9]+),(?P<z>-?[0-9]+.[0-9]+)")
regex_kinect = re.compile("(?P<number>[0-9]+);(?P<x>-?[0-9]+.[0-9]+);(?P<y>-?[0-9]+.[0-9]+);(?P<z>-?[0-9]+.[0-9]+)")
regex_kinect_time = re.compile("E;(?P<time>[0-9]{13})")
kinect_pos = []
kinect_acc = []
android_acc = []
merged_data = []
FREQ = 3

TEST = False
offset = None

cvs_file = open('some.csv', 'w', newline='')
fieldnames = ['time', 'android']
fieldnames.extend(list(range(21)))
writer = csv.DictWriter(cvs_file, fieldnames=fieldnames)
writer.writeheader()

def lowPassFilter(data, old_data, dt, freq):
    RC = 1/(2*math.pi*freq)
    a = dt / (RC + dt)
    d = a*data + (1-a)*old_data
    #print('lowPass: dt='+ str(dt) + '\n\t' + str(1-a)+'*'+str(old_data) + ' + '+ str(a)+'*'+str(data)+'\n\t-> '+str(d))
    return d

def mergeData():
    if len(kinect_acc) > 0:
        kinect = kinect_acc[0]
        found_match = False
        found_index = -1
        for i, android in enumerate(android_acc):
            if android[0] > kinect[0]:
                found_index = i
                break
        #print('found index : '+str(found_index))
        if found_index != -1:
            merged_data.append((android_acc[found_index], kinect)) # MAYBE: mean android_acc
            global offset
            if offset is None:
                offset = android_acc[found_index][0]
            temp = {'time': android_acc[found_index][0]-offset, 'android': android_acc[found_index][1]}
            temp.update(kinect[1])
            writer.writerow(temp)
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
        #print("data")
        acc_android = data[0][1]
        acc_kinect_dict = data[1][1]
        s_acc_android = math.sqrt(acc_android.x*acc_android.x+acc_android.y*acc_android.y+acc_android.z*acc_android.z)
        for key, value in acc_kinect_dict.items():
            #print("value")
            if key not in diff_dict:
                diff_dict[key] = ([], [])
            s_acc_kinect = math.sqrt(value.x*value.x+value.y*value.y+value.z*value.z)
            # ratio = s_acc_android/s_acc_kinect
            diff_dict[key][0].append(s_acc_android)
            diff_dict[key][1].append(s_acc_kinect)
            # print("key")
            # print(key)
            # print(s_acc_kinect)

    var_dict = {}
    for key, value in diff_dict.items():
        if len(value[0]) > 2:
            # print(key)
            # print(value)
            v = numpy.correlate(value[0][-10:], value[1][-10:])
            # v = scipy.stats.pearsonr(value[0][-10:], value[1][-10:])
            v = v[0]
            print(str(key)+':'+str(v))
            var_dict[key] = v

    if var_dict:
        best_key = max(var_dict, key=var_dict.get)
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
    b = b'1234567890000,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890010,ACCEL,10.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890020,ACCEL,10.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890030,ACCEL,10.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890040,ACCEL,10.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890050,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890060,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)
    b = b'1234567890070,ACCEL,1.0,1.0,1.0\n'
    server_android.data_received(b)

def testKinect():
    client_kinect = ClientKinect()
    a = b"E;1234567890000\n"
    client_kinect.data_received(a)
    a = b"1;1.1;1.1;1.1E;1234567890000\n"
    client_kinect.data_received(a)
    a = b"1;10.2;1.1;1.1E;1234567890010\n"
    client_kinect.data_received(a)
    a = b"1;10.3;1.1;1.1E;1234567890020\n"
    client_kinect.data_received(a)
    a = b"1;1.4;1.1;1.12;2;0.0;0.0;0.0E;1234567890030\n1;1.4;1.1;1.12;2;0.0;0.0;0.0E;1234567890030\n1;1.4;1.1;1.12;2;0.0;0.0;0.0E;1234567890030\n"
    client_kinect.data_received(a)
    a = b"1;1.5;1.1;1.12;2;1.0;1.0;1.0E;1234567890040\n"
    client_kinect.data_received(a)
    a = b"1;10.6;1.1;1.12;2;3.0;3.0;3.0E;1234567890050\n"
    client_kinect.data_received(a)
    a = b"1;10.7;1.1;1.12;2;6.0;6.0;6.0E;1234567890060\n"
    client_kinect.data_received(a)
    a = b"1;1.8;1.1;1.12;2;10.0;10.0;10.0E;1234567890070\n"
    client_kinect.data_received(a)

class Coord:
    def __init__(self, x, y, z):
        self.x = float(x)
        self.y = float(y)
        self.z = float(z)

    def __str__(self):
        return str(math.sqrt(self.x*self.x+self.y*self.y+self.z*self.z))

class MI12:
    def __init__(self):
        if not TEST:
            loop = asyncio.get_event_loop()
            server_android = loop.create_server(ServerAndroid, '172.25.13.82', 11337)
            client_kinect = loop.create_connection(ClientKinect, '172.25.13.82', 8888)
            loop.run_until_complete(server_android)
            loop.run_until_complete(client_kinect)
            loop.run_forever()
        else:
            testAndroid()
            testKinect()

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
        splited = str(kinect_read_buffer).split('\\n\'')
        for data in splited:
            # print(str(data))
            extracted_data = self.extractData(str(data))
            if extracted_data is not None:
                # print(extracted_data)
                kinect_read_buffer = ""
                filtred_extracted_data = self.filter(extracted_data)
                # print(filtred_extracted_data[1])
                # print(str(extracted_data[1][1].x)+str('\t')+str(filtred_extracted_data[1][1].x))
                kinect_pos.append(filtred_extracted_data)
                if len(kinect_pos) == 3 :
                    acc = self.compute_acc(kinect_pos[0], kinect_pos[1], kinect_pos[2])
                    #filtred_acc = self.filter(acc)
                    kinect_acc.append(acc)
                    del kinect_pos[0]
                    mergeData()
                    choosen = choose()
                    if not TEST:
                        self.transport.write((str(choosen)+str('\n')).encode())
            else:
                kinect_read_buffer = data
                # print("Wrong kinect data")

    def extractData(self, raw_input):
        time_group = regex_kinect_time.search(raw_input)
        coord_match = regex_kinect.finditer(raw_input)
        if coord_match and time_group:
            time = float(time_group.group('time'))/1000.0
            coords = {}
            for coord in coord_match:
                # print(int(coord.group('number')))
                c = Coord(coord.group('x'), coord.group('y'), coord.group('z'))
                coords[int(coord.group('number'))] = c
                # print(str(time)+str(c))
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

    def filter(self, data, f=FREQ):
        if not hasattr(self, 'old_data'):
            self.old_data = None
        
        if self.old_data is None:
            self.old_data = data
            return data
        else:
            dict_data = data[1]
            new_data = {}
            for key, value in dict_data.items():
                if key in self.old_data[1] :
                    print(str(value)+ ' ; '+str(self.old_data[1][key]))
                    new_data[key] = Coord( \
                            lowPassFilter(value.x, self.old_data[1][key].x, data[0]-self.old_data[0], f), \
                            lowPassFilter(value.y, self.old_data[1][key].y, data[0]-self.old_data[0], f), \
                            lowPassFilter(value.z, self.old_data[1][key].z, data[0]-self.old_data[0], f)\
                        )
                else:
                    new_data[key] = value

            self.old_data = (data[0], new_data)
            return self.old_data

class ServerAndroid(asyncio.Protocol):
    def connection_made(self, transport):
        peername = transport.get_extra_info('peername')
        print('Android connected from {}'.format(peername))
        self.transport = transport

    def connection_lost(self, exc):
        print('Android disconnected')

    def data_received(self, raw_data):
        global android_read_buffer
        # android_read_buffer += str(raw_data)
        splited = str(raw_data).split("\\n")
        for data in splited:
            # print(str(data))
            extracted_data = self.extractData(str(data))
            if extracted_data is not None:
                # android_read_buffer = ""
                filtred_extracted_data = self.filter(extracted_data)
                # print(str(extracted_data[1].x)+str('\t')+str(filtred_extracted_data[1].x))
                android_acc.append(filtred_extracted_data)
            # else:
                # android_read_buffer = data

    def extractData(self, raw_input):
        data = regex_android.search(raw_input)
        if data:
            return (float(data.group('time'))/1000.0, Coord(data.group('x'), data.group('y'), data.group('z')))
        else:
            return None

    def filter(self, data, f=FREQ):
        if not hasattr(self, 'old_data'):
            self.old_data = None
        
        if self.old_data is None:
            self.old_data = data
            return data

        new_data =  (\
                        data[0], \
                        Coord(\
                            lowPassFilter(data[1].x, self.old_data[1].x, data[0]-self.old_data[0], f), \
                            lowPassFilter(data[1].y, self.old_data[1].y, data[0]-self.old_data[0], f), \
                            lowPassFilter(data[1].z, self.old_data[1].z, data[0]-self.old_data[0], f)\
                        )\
                    )

        self.old_data = new_data
        return new_data

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
