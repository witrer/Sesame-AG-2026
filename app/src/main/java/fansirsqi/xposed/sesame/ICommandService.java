package fansirsqi.xposed.sesame;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ICommandService extends IInterface {
    void executeCommand(String command, ICallback callback) throws RemoteException;
    void registerListener(IStatusListener listener) throws RemoteException;
    void unregisterListener(IStatusListener listener) throws RemoteException;

    abstract class Stub extends Binder implements ICommandService {
        private static final String DESCRIPTOR = "fansirsqi.xposed.sesame.ICommandService";
        static final int TRANSACTION_executeCommand = 1;
        static final int TRANSACTION_registerListener = 2;
        static final int TRANSACTION_unregisterListener = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICommandService asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof ICommandService) return (ICommandService) iin;
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION:
                    reply.writeString(descriptor);
                    return true;
                case TRANSACTION_executeCommand:
                    data.enforceInterface(descriptor);
                    String command = data.readString();
                    ICallback callback = ICallback.Stub.asInterface(data.readStrongBinder());
                    executeCommand(command, callback);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_registerListener:
                    data.enforceInterface(descriptor);
                    IStatusListener listener = IStatusListener.Stub.asInterface(data.readStrongBinder());
                    registerListener(listener);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_unregisterListener:
                    data.enforceInterface(descriptor);
                    IStatusListener listener2 = IStatusListener.Stub.asInterface(data.readStrongBinder());
                    unregisterListener(listener2);
                    reply.writeNoException();
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ICommandService {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void executeCommand(String command, ICallback callback) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(command);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_executeCommand, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void registerListener(IStatusListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerListener, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void unregisterListener(IStatusListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    mRemote.transact(TRANSACTION_unregisterListener, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }
    }
}
