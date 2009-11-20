BITS=32

ifeq ($(BITS),32)
  CC=gcc32
  NS=win-x86
else
  BITS=64
  CC=gcc64
  NS=win-x64
endif

CFLAGS=-m$(BITS) -shared -c -fno-rtti -fPIC -I/usr/lib/jvm/java-6-sun/include -I../common/platform-libs/jre-include/win32 -Iresources/includes
LFLAGS=-m$(BITS) -shared -fno-rtti -fPIC -L../common/platform-libs/OpenCL/win/_x64 -lOpenCL

SRC=gensrc/native/CLImpl_JNI.c
OBJ=build/obj/jocl/$(NS)/CLImpl_JNI.o
BIN=build/natives/jocl/$(NS)/jocl.dll

all: $(BIN)

$(BIN): $(OBJ)
	$(CC) $(LFLAGS) $(OBJ) -o $(BIN)

$(OBJ): $(SRC)
	$(CC) $(CFLAGS) $(SRC) -o $(OBJ)

clean:
	rm -f $(BIN) $(OBJ)

