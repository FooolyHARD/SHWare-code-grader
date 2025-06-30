package com.example;

public interface Vehicle {
    void startEngine();
    void stopEngine();
    void fly();  // Пример метода, который нарушает ISP
    void drive();  // Пример метода, который нарушает ISP
}

public class Car implements Vehicle {
    @Override
    public void startEngine() {
        System.out.println("Car engine started");
    }

    @Override
    public void stopEngine() {
        System.out.println("Car engine stopped");
    }

    @Override
    public void drive() {
        System.out.println("Car is driving");
    }

    @Override
    public void fly() {
        throw new UnsupportedOperationException("Cars cannot fly");
    }
}

public class Plane implements Vehicle {
    @Override
    public void startEngine() {
        System.out.println("Plane engine started");
    }

    @Override
    public void stopEngine() {
        System.out.println("Plane engine stopped");
    }

    @Override
    public void fly() {
        System.out.println("Plane is flying");
    }

    @Override
    public void drive() {
        throw new UnsupportedOperationException("Planes cannot drive");
    }
}