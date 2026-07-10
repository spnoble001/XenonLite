# Xenon Lite

Esqueleto de mod de cliente para Minecraft Forge 1.8.9.

## Requisitos

- JDK 8 (ForgeGradle 2.1 no funciona correctamente con JDK modernos).
- Gradle compatible con ForgeGradle 2.1, o un wrapper generado para el proyecto.

## Preparación

```bash
gradle setupDecompWorkspace
gradle idea
```

Para iniciar el cliente de desarrollo:

```bash
gradle runClient
```

Para producir el JAR:

```bash
gradle build
```

## Módulo Left Clicker

`LeftClicker` se registra en el bus de eventos durante la inicialización y queda
desactivado por defecto. Cuando el gestor de módulos o la interfaz estén listos,
puede activarse con:

```java
XenonLite.instance.getLeftClicker().setEnabled(true);
XenonLite.instance.getRightClicker().setEnabled(true);
XenonLite.instance.getRefill().setEnabled(true);
XenonLite.instance.getThrowPot().toggle();
XenonLite.instance.getAimAssist().setEnabled(true);
```

Si no se usa una instancia estática de `XenonLite`, conserva la instancia que Forge
inyecta o conecta el método `setEnabled` directamente desde tu gestor de módulos.
