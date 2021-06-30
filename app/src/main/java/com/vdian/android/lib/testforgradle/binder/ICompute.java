package com.vdian.android.lib.testforgradle.binder;

/**
 * @author yulun
 * @sinice 2021-06-23 20:03
 */
public interface ICompute extends android.os.IInterface {
    public int add(int a, int b) throws android.os.RemoteException;

    /**
     * Local-side IPC implementation stub class.
     */
    public static abstract class Stub extends android.os.Binder implements com.vdian.android.lib.testforgradle.binder.ICompute {
        static final int TRANSACTION_add = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        private static final java.lang.String DESCRIPTOR = "com.vdian.android.lib.testforgradle.binder.ICompute";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            android.util.Log.i("remoteTest", "test binder is new Stub");
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.vdian.android.lib.testforgradle.binder.ICompute interface,
         * generating a proxy if needed.
         */
        public static com.vdian.android.lib.testforgradle.binder.ICompute asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.util.Log.i("remoteTest", "test binder asInterface "+obj);
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.vdian.android.lib.testforgradle.binder.ICompute))) {
                return ((com.vdian.android.lib.testforgradle.binder.ICompute) iin);
            }
            return new com.vdian.android.lib.testforgradle.binder.ICompute.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            java.lang.String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_add: {
                    data.enforceInterface(descriptor);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    int _result = this.add(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements com.vdian.android.lib.testforgradle.binder.ICompute {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public int add(int a, int b) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(a);
                    _data.writeInt(b);
                    mRemote.transact(Stub.TRANSACTION_add, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}
