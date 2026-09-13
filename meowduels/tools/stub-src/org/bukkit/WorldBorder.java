package org.bukkit;

public interface WorldBorder {
    double getSize();
    void setCenter(double a0, double a1);
    void setSize(double a0);
    void setSize(double a0, long a1);
    void setWarningDistance(int a0);
    void setWarningTime(int a0);
}
