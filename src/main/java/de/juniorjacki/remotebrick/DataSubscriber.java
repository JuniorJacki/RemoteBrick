package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.ConnectedDevice;

import java.util.concurrent.CopyOnWriteArrayList;

public class DataSubscriber {

    static CopyOnWriteArrayList<SimpleDataListener> simpleDataListeners = new CopyOnWriteArrayList<>();
    static CopyOnWriteArrayList<ComplexDataListener> complexDataListeners = new CopyOnWriteArrayList<>();


    public interface DataConsumer {
        void consume(Object newValue);
    }

    public interface ComplexDataConsumer {
        void consume(Object newKeyValue,Object newValue);
    }

    record SimpleDataListener<T extends ConnectedDevice<M>,M extends Enum<M> & ConnectedDevice.DataType>(T device, M type, DataConsumer dataConsumer,int priority) {
        public void newData(Object newValue) {
            new Thread(() -> dataConsumer.consume(newValue)).start();
        }
    }

    record ComplexDataListener<T extends ConnectedDevice<M>,M extends Enum<M> & ConnectedDevice.DataType,S extends ConnectedDevice<Z>,Z extends Enum<Z> & ConnectedDevice.DataType>(T keyDevice,M keyValueType,S valueDevice,Z valueType,ComplexDataConsumer dataConsumer,int priority) {
        public void newData(Object newKeyValue,Object newValue) {
            new Thread(() -> dataConsumer.consume(newKeyValue,newValue)).start();
        }
    }


    /**
     * Default Priority = 0 - Smallest Values are handled before Higher Values
     */
    public static <T extends ConnectedDevice<M>,M extends Enum<M> & ConnectedDevice.DataType> SimpleDataListener<T,M> registerSimpleListener(T device,M type,DataConsumer dataConsumer) {
        return registerSimpleListener(device,type,dataConsumer,0);
    }

    public static <T extends ConnectedDevice<M>,M extends Enum<M> & ConnectedDevice.DataType> SimpleDataListener<T,M> registerSimpleListener(T device,M type,DataConsumer dataConsumer,int priority) {
        if (device.getDeviceRoot() == null) throw new IllegalArgumentException("Device Hub is null");
        if (!device.isFunctional()) throw new IllegalArgumentException("Device is not Functional");
        SimpleDataListener<T,M> listener = new SimpleDataListener<>(device,type,dataConsumer,priority);
        simpleDataListeners.add(listener);
        return listener;
    }

    public static boolean unregisterSimpleListener(DataConsumer dataConsumer) {
        return simpleDataListeners.removeIf(simpleDataListener ->  simpleDataListener.dataConsumer.equals(dataConsumer));
    }

    /**
     * Default Priority = 0 - Smallest Values are handled before Higher Values
     */
    public static  <T extends ConnectedDevice<M>,M extends Enum<M> & ConnectedDevice.DataType,S extends ConnectedDevice<Z>,Z extends Enum<Z> & ConnectedDevice.DataType> ComplexDataListener<T,M,S,Z> registerComplexListener(T keyDevice,M keyValueType,S valueDevice,Z valueType,ComplexDataConsumer dataConsumer) {
        return registerComplexListener(keyDevice,keyValueType,valueDevice,valueType,dataConsumer,0);
    }

    public static  <T extends ConnectedDevice<M>,M extends Enum<M> & ConnectedDevice.DataType,S extends ConnectedDevice<Z>,Z extends Enum<Z> & ConnectedDevice.DataType> ComplexDataListener<T,M,S,Z> registerComplexListener(T keyDevice,M keyValueType,S valueDevice,Z valueType,ComplexDataConsumer dataConsumer,int priority) {
        if (keyDevice.getDeviceRoot() == null || valueDevice.getDeviceRoot() == null) throw new IllegalArgumentException("Device Hub is null");
        if (!keyDevice.isFunctional() || !valueDevice.isFunctional()) throw new IllegalArgumentException("Device is not Functional");
        ComplexDataListener<T,M,S,Z> listener = new ComplexDataListener<>(keyDevice,keyValueType,valueDevice,valueType,dataConsumer,priority);
        complexDataListeners.add(listener);
        return listener;
    }

    public static boolean unregisterComplexListener(ComplexDataConsumer dataConsumer) {
        return complexDataListeners.removeIf(complexDataListener ->  complexDataListener.dataConsumer.equals(dataConsumer));
    }
}
