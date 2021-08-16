package com.mawujun.ble.usb;

import javax.usb.*;
import javax.usb.util.UsbUtil;
import java.util.List;

public class UsbConn {
    private static final short VENDOR_ID = 2578;//指定供应商
    private static final short PRODUCT_ID = 1;//指定产品
    private static final byte ENDPOINT_OUT= 0x03;
    private static final byte ENDPOINT_IN= (byte) 0x83;

    private static final byte INTERFACE_AD= 0x01;

    private static final byte[] COMMAND = {0x01,0x00};

    @SuppressWarnings("unchecked")
    public static UsbDevice findMissileLauncher(UsbHub hub) {
        UsbDevice launcher = null;

        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
            if (device.isUsbHub()) {
                launcher = findMissileLauncher((UsbHub) device);
                if (launcher != null)
                    return launcher;
            } else {
                UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
                //显示结果:Bus 001 Device 014:ID 17ef:608d
                //17ef是16进制的idVendor(供应商)，608d是16进制的产品号
                System.out.println("设备信息:"+device);
                System.out.println("idVendor=" + desc.idVendor() +",idProduct="+desc.idProduct());
                if (desc.idVendor() == VENDOR_ID && desc.idProduct() == PRODUCT_ID) {
                    System.out.println("发现设备" + device);
                    return device;
                }
            }
        }
        return null;
    }
    //command for controlTransfer
    public static void sendMessage(UsbDevice device, byte[] message)
            throws UsbException {
        UsbControlIrp irp = device
                .createUsbControlIrp(
                        (byte) (UsbConst.REQUESTTYPE_TYPE_CLASS | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                        (byte) 0x09, (short) 2, (short) 1);
        irp.setData(message);
        device.syncSubmit(irp);
    }
    /**
     * Class to listen in a dedicated Thread for data coming events.
     * This really could be used for any HID device.
     */
    public static class HidMouseRunnable implements Runnable
    {
        /* This pipe must be the HID interface's interrupt-type in-direction endpoint's pipe. */
        public HidMouseRunnable(UsbPipe pipe) { usbPipe = pipe; }
        public void run()
        {
            byte[] buffer = new byte[UsbUtil.unsignedInt(usbPipe.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];
            @SuppressWarnings("unused")
            int length = 0;

            while (running) {
                try {
                    length = usbPipe.syncSubmit(buffer);
                } catch ( UsbException uE ) {
                    if (running) {
                        System.out.println("Unable to submit data buffer to HID mouse : " + uE.getMessage());
                        break;
                    }
                }
                if (running) {
//                    System.out.print("Got " + length + " bytes of data from HID mouse :");
//                    for (int i=0; i<length; i++)
//                        System.out.print(" 0x" + UsbUtil.toHexString(buffer[i]));
                    try {
                        //String result = DataFix.getHexString(buffer);
                        String result ="未理解:还不知道上面为什么这样";//只注释了一行
                        System.out.println(result);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        /**
         * Stop/abort listening for data events.
         */
        public void stop()
        {
            running = false;
            usbPipe.abortAllSubmissions();
        }
        public boolean running = true;
        public UsbPipe usbPipe = null;
    }
    /**
     * get the  correct Interface for USB
     * @return
     * @throws UsbException
     */
    public static UsbInterface readInit() throws UsbException{
        UsbDevice device = findMissileLauncher(UsbHostManager.getUsbServices()
                .getRootUsbHub());
        if (device == null) {
            System.out.println("Missile launcher not found.");
            System.exit(1);
        }
        UsbConfiguration configuration = device.getActiveUsbConfiguration();
        for (UsbInterface iface: (List<UsbInterface>) configuration.getUsbInterfaces())
        {
            System.out.println(iface.getUsbInterfaceDescriptor());
            try {
                iface.claim();
            } catch (UsbException e) {
                e.printStackTrace();
                continue;
            }
            return iface;
        }
        return null;


//        UsbInterface iface = configuration.getUsbInterface(INTERFACE_AD);//Interface Alternate Number
//        iface.claim();
////        //if you using the MS os,may be you need judge,because MS do not need follow code,by tong
////        iface.claim(new UsbInterfacePolicy() {
////            @Override
////            public boolean forceClaim(UsbInterface arg0) {
////                // TODO Auto-generated method stub
////                return true;
////            }
////        });
//        return iface;
    }
    /**
     * 异步bulk传输,by tong
     * @param usbInterface
     * @param data
     */
    public static void syncSend(UsbInterface usbInterface,byte[] data) {
        UsbEndpoint endpoint = usbInterface.getUsbEndpoint(ENDPOINT_OUT);
        UsbPipe outPipe = endpoint.getUsbPipe();
        try {
            outPipe.open();
            outPipe.syncSubmit(data);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            try {
                outPipe.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static HidMouseRunnable listenData(UsbInterface usbInterface) {
        UsbEndpoint endpoint = usbInterface.getUsbEndpoint(ENDPOINT_IN);
        UsbPipe inPipe = endpoint.getUsbPipe();
        HidMouseRunnable hmR = null;
        try {
            inPipe.open();
            hmR = new HidMouseRunnable(inPipe);
            Thread t = new Thread(hmR);
            t.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return hmR;
    }
    /**
     * 主程序入口
     *
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        UsbInterface iface;
        try {
            iface = readInit();
            listenData(iface);
            syncSend(iface, COMMAND);
        } catch (UsbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
