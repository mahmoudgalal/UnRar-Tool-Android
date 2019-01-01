# -Wall
# -DRAR_NOCRYPT
#-DNOVOLUME 
#-UMBFUNCTIONS  -fvisibility=hidden  -UUNICODE_SUPPORTED -UMBFUNCTIONS
LOCAL_PATH :=$(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := unrar
LOCAL_CPPFLAGS += -DLITTLE_ENDIAN
LOCAL_CFLAGS +=  -DRARDLL   -DLITTLE_ENDIAN \
  #-D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE
#-fshort-wchar

LOCAL_CPP_FEATURES := exceptions

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SRC_FILES +=      filestr.cpp scantree.cpp dll.cpp \
        rar.cpp strlist.cpp strfn.cpp pathfn.cpp savepos.cpp smallfn.cpp \
        global.cpp file.cpp filefn.cpp filcreat.cpp archive.cpp arcread.cpp \
        unicode.cpp system.cpp isnt.cpp crypt.cpp crc.cpp rawread.cpp encname.cpp \
        resource.cpp match.cpp timefn.cpp rdwrfn.cpp consio.cpp options.cpp \
        ulinks.cpp errhnd.cpp rarvm.cpp rijndael.cpp getbits.cpp sha1.cpp \
        extinfo.cpp extract.cpp volume.cpp list.cpp find.cpp unpack.cpp \
        cmddata.cpp secpassword.cpp


LOCAL_LDLIBS := -llog

LOCAL_LDFLAGS := -Wl,--as-needed
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := unrardyn
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
#LOCAL_CFLAGS += -fshort-wchar
LOCAL_CFLAGS +=  -DRARDLL   -DLITTLE_ENDIAN \
#-D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE
LOCAL_SRC_FILES = unrardyn.cpp

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := unrar
include $(BUILD_SHARED_LIBRARY)



