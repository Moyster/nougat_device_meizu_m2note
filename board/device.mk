# Force linking shim
LINKER_FORCED_SHIM_LIBS := /system/lib/libshowlogo.so|libshim_asc.so:/system/lib/libmedia.so|libshim_snd.so:/system/lib64/libmedia.so|libshim_snd.so:/system/lib/liblog.so|libshim_xlog.so:/system/lib64/liblog.so|libshim_xlog.so:/system/lib/libui.so|libshim_ui.so:/system/lib64/libui.so|libshim_ui.so:/system/lib/libgui.so|libshim_gui.so:/system/lib64/libgui.so|libshim_gui.so:/system/bin/mtk_agpsd|libshim_agps.so

## Fun with flags
BOARD_USES_MTK_HARDWARE := true

# Camera flags
BOARD_GLOBAL_CFLAGS += -DCAMERA_VENDOR_L_COMPAT
TARGET_CAMERASERVICE_CLOSES_NATIVE_HANDLES := true
