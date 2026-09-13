# VoicePocket — Roadmap

## Visión

VoicePocket tiene como propósito permitir al usuario capturar una idea antes de que desaparezca, mediante una experiencia rápida, simple y centrada en la voz.

El desarrollo futuro debe conservar el flujo principal:

Abrir → Hablar → Guardar

Las nuevas funcionalidades no deben agregar pasos obligatorios a este proceso.

---

## Estado actual

### Funcionalidades implementadas

- Captura de ideas mediante voz.
- Almacenamiento local de ideas.
- Visualización cronológica.
- Búsqueda de ideas.
- Edición manual.
- Eliminación con confirmación.
- Copiar contenido.
- Compartir contenido.
- Estado vacío.
- Tema claro y oscuro.
- Design System propio.
- Microinteracciones en el botón de captura.
- Animaciones en los cambios de la lista.

---

## Hallazgos de uso real

Las pruebas realizadas durante un periodo prolongado con usuarios reales mostraron que el flujo principal de captura funciona correctamente y resulta fácil de utilizar.

Sin embargo, aparecieron nuevas necesidades a medida que aumentó la cantidad de información almacenada:

1. Las ideas antiguas pueden quedar olvidadas.
2. Una lista cronológica no es suficiente para organizar una cantidad mayor de ideas.
3. La búsqueda ayuda cuando el usuario recuerda qué está buscando, pero no reemplaza una estructura de organización.
4. Los usuarios utilizan VoicePocket espontáneamente para crear listas de tareas.
5. Editar una nota mediante teclado resulta incómodo cuando solamente se desea agregar nueva información.
6. Las pausas durante un dictado pueden finalizar la captura antes de que el usuario termine de desarrollar su idea.

---

## Próximas funcionalidades

### Prioridad 1 — Carpetas personalizadas

Permitir al usuario crear carpetas con nombres propios para organizar sus ideas.

Ejemplos:

- Trabajo
- Casa
- Auto
- Colegio
- Proyectos

Las ideas sin clasificación permanecerán disponibles en "Sin carpeta".

La creación de una idea seguirá siendo inmediata y no será obligatorio seleccionar una carpeta antes de capturarla.

### Prioridad 2 — Organización de ideas

Permitir mover una idea existente entre carpetas sin modificar su contenido.

Debe existir una vista "Todas las ideas" para conservar acceso global a la información.

### Prioridad 3 — Agregar por voz

Permitir continuar una idea existente mediante una nueva grabación de voz.

La función "Editar" se mantendrá para corregir errores o modificar manualmente el texto.

### Prioridad 4 — Captura extendida

Mejorar el comportamiento de la captura cuando el usuario realiza pausas mientras piensa o dicta información extensa.

### Prioridad 5 — Listas / Checklist

Permitir que determinadas ideas puedan utilizarse como listas cuyos elementos puedan marcarse individualmente sin necesidad de editar el texto completo.

### Prioridad 6 — Recuperación de ideas

Investigar mecanismos que ayuden al usuario a reencontrar información que ha quedado olvidada.

Posibles alternativas:

- Ideas recientes.
- Recordatorios opcionales.
- Ideas que no se han revisado durante un tiempo.

Esta funcionalidad deberá diseñarse evitando notificaciones innecesarias.

---

## Funcionalidades posteriores

- Favoritos.
- Etiquetas.
- Exportación.
- Sincronización.
- Funciones asistidas por IA.

Estas funcionalidades no forman parte de la prioridad actual.

---

## Branding y publicación

La identidad visual Pocket Spark, icono definitivo, splash screen y preparación de publicación se retomarán después de completar y validar las mejoras prioritarias de experiencia de usuario.

---

## Principio de producto

> Capturar debe seguir siendo inmediato. Organizar debe ser opcional. Recuperar debe ser fácil.