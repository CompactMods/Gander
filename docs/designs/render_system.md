# Gander Render System

There are three basic goals:

- Correctness: We should aim to do the right thing, highlighting errors as
  quickly as possible.

- Extensible: Allow mods to configure how most, if not all of how the game is
  rendered.

- Performance: We should aim to be speedy. Duh.

## High Level Overview

We are concerned with the following aspects of rendering:
- Allocating resources efficiently
- Scheduling tasks (e.g. dispatching compute, issuing draw calls, blitting
  framebuffers...)
- Dependency management - ensuring that resources and passes exist, if they are
  requested

To do this, we're borrowing the concept of the venerable frame graph but there
are some important changes we need to make:
1. Most implementations are designed with a single source-of-truth, roughly
   linear code flow in mind. Putting render passes into separate classes is
   (usually) discouraged as it makes code harder to follow.

   Unfortunately, this is a modded scenario. It's impossible to keep everything
   in one file, because we're trying to reconcile the needs of every modder. So
   instead, we should embrace this and design a solution that works best in
   multiple files.

2. Generally, the frame graph lasts only for the one frame (it's in the name!)
   with state stored externally (e.g. in a blackboard) and is re-created for
   every frame.

   We'd like to avoid this extra overhead from object churn by instead doing
   this only once.[1]

3. We need to allow components to be replaced, while still keeping the
   guarantees of inputs and outputs to downstream render passes.

   This is to enable mods to provide alternative implementations providing
   additional features (e.g. deferred shading and advanced path-traced
   lighting) while remaining mostly compatible with mods. There will always be
   a small selection of techniques which will always be incompatible, but we'd
   like to get most mods talking on the same page.

[1]: This would likely mean "as needed", that is, it only gets re-created when
signalled. (e.g. due to a window resize or due to changing a graphics setting)

## The Frame Graph

Frame graphs have their origins in [a 2017 GDC talk][GDCFrameGraph] and have
made their way into a large number of game engines in some form or another. In
fact, there is even an implementation already in Blaze3D (`com.mojang.blaze3d.framegraph`)
but it has, at least, the issues listed above which make it inadequate for our
use.

To a user, most of the implementation of the frame graph will be hidden, as it
shouldn't be necessary[1] to poke at to get something done. Instead, we'll
provide these main APIs to them:

- Registering render passes
- Registering resources
- Registering blackboard (i.e. shared between render passes) entries

[1]: Of course, this is subject to change based on concrete use-cases.

### Registering Render Passes

To register a render pass, the user should do something like this:

```java
renderSystemConfig.registerPass("name")
    // Configure inputs and outputs
    .addInput("input")
    .addOutput("depth", Texture2DResource.builder()
        .dimensions(width, height)
        .format(/* Any OpenGL texture format constant */))

    // Called as needed to configure the resources the pass creates
    .configure((blackboard, context, resources) -> {
        // e.g. configuring a texture resource to use the same dimensions as
        // the input.
        var input = resources.get("input");
        context.resource("depth", Texture2DResource.class)
            .dimensions(input.width(), input.height());
    })

    // Called every frame to actually render.
    .executes((blackboard, context, resources) -> {
        // Read some shared state between passes
        var state = blackboard.get(SharedData.class);

        // Get the input and output resources
        var input = resources.get("input");
        var buffer = resources.get("depth");

        // Set up render state, dispatch draw calls, etc.
    });
```

### Registering Resources

To register a resource, the user should do something like this:

```java
renderSystemConfig.registerResource("name",
    // Configure the defaults/type of the resource
    Texture2DResource.builder()
        .dimensions(width, height)
        .format(/* Any OpenGL texture format constant */))

    // Called as needed to configure the resource (e.g. if a reload is
    // requested)
    .configure((context, resource) -> {
        resource.dimensions(
            context.window().width(),
            context.window().height());
    });
```

### Registering Blackboard Entries

To register a blackboard entry, the user should do something like this:

```java
renderSystemConfig.registerBlackboard(SharedData.class)
    /* .readableAs(SomeSharedData.class) */
    // Called when the first
    .configure(context -> {
        var result = new SharedData();

        result.averageBrightness = 0;

        return result;
    });

class SharedData
{
    public float averageBrightness;
}
```

[GDCFrameGraph]: https://www.gdcvault.com/play/1024045/FrameGraph-Extensible-Rendering-Architecture-in
