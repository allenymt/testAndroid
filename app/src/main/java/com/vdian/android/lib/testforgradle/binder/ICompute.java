package com.vdian.android.lib.testforgradle.binder;

/**
 * @author yulun
 * @sinice 2021-06-23 20:03
 */
public interface ICompute extends android.os.IInterface {
    public int add(int a, int b) throws android.os.RemoteException;

    /**
     * Local-side IPC implementation stub class.
     * 在remote进程里的对象，注意这个类是abstract的，也就是说ICompute这个接口stub本身并没有实现，真正的实现是在远端进程里
     */
    public static abstract class Stub extends android.os.Binder implements com.vdian.android.lib.testforgradle.binder.ICompute {
        static final int TRANSACTION_add = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        private static final java.lang.String DESCRIPTOR = "com.vdian.android.lib.testforgradle.binder.ICompute";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            android.util.Log.i("remoteTest", "test binder is new Stub");
            // 初始化的时候把自身也就是binder和描述(可以列为key)，注入进去，在asInterface里会调用
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.vdian.android.lib.testforgradle.binder.ICompute interface,
         * generating a proxy if needed.
         * TODO 这个方法需要详细理解下
         * 入参就是Server进程在当前进程的影子对象，类型是IBinder是因为所有的此类对象的父类都是IBinder
         */
        public static com.vdian.android.lib.testforgradle.binder.ICompute asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.util.Log.i("remoteTest", "test binder asInterface "+obj);
            // 在Stub对象初始化的时候就绑定了DESCRIPTOR，注意绑定发生在server进程
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);

            if (((iin != null) && (iin instanceof com.vdian.android.lib.testforgradle.binder.ICompute))) {
                // 这里应该是在当前也就是Server进程调的时候才走这里
                // 那为什么client进程走不到这里呢，因为client进程里 iin取到的是空，进程间内存是隔离的，iin只有在server进程才能取到值
                android.util.Log.i("remoteTest", "test binder asInterface2 "+obj);
                return ((com.vdian.android.lib.testforgradle.binder.ICompute) iin);
            }
            // 在当前进程也就是client进程调用走这里，实际返回的是个代理对象
            return new com.vdian.android.lib.testforgradle.binder.ICompute.Stub.Proxy(obj);
        }

        // 本身就是binder对象,因为继承的是android.os.Binder
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
                    // 从其他进程过来的数据是序列化的Parcel，主要当前进程是远程进程
                    data.enforceInterface(descriptor);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    // 这里调用的就是具体业务实现的方法
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

        // 这是在用户进程的对象，这个对象存在的意义是什么？
        // 这个对象可以理解为 stub在用户进程的影子
        private static class Proxy implements com.vdian.android.lib.testforgradle.binder.ICompute {
            // 远程binder对象在本进程中的影子
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

            // 实现接口里的方法。进程间的数据都是通过Parcel序列化的，所以如果是自定义对象，在对方进程和自身进程都要存在
            @Override
            public int add(int a, int b) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(a);
                    _data.writeInt(b);
                    // 调用Server进程的方法，同时把数据传递过去
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
