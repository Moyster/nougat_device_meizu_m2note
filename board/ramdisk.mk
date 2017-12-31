# device rootdir
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/rootdir/fstab.mt6735:root/fstab.mt6735 \
    $(DEVICE_PATH)/rootdir/fstab.swap:root/fstab.swap \
    $(DEVICE_PATH)/rootdir/init.modem.rc:root/init.modem.rc \
    $(DEVICE_PATH)/rootdir/init.mt6735.rc:root/init.mt6735.rc \
    $(DEVICE_PATH)/rootdir/init.mt6735.usb.rc:root/init.mt6735.usb.rc \
    $(DEVICE_PATH)/rootdir/ueventd.mt6735.rc:root/ueventd.mt6735.rc \
    $(DEVICE_PATH)/rootdir/init.recovery.mt6735.rc:root/init.recovery.mt6735.rc

# hack for TWRP
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/recovery/twrp.fstab:recovery/root/etc/twrp.fstab
