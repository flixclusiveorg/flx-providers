# Provider Template

This is a template repository for creating a custom provider for the [Flixclusive](https://github.com/flixclusiveorg/Flixclusive) applicatoin.

⚠️ **Important:** When using this template, ensure you check the **"Include all branches"** option. The **build** branch is essential for GitHub CI integration, which automates the build process of the providers.

## What is a Provider?

A **provider** is an extension or addon that allows you to fetch media content (like movies or shows) from various sources. For Flixclusive, these providers act like browser extensions that help gather media links from different platforms.

If you're new to coding, think of a provider as a bridge that connects a streaming service (like Netflix or Amazon Prime) to the Flixclusive app, allowing it to display content from that service.

## Getting Started

Follow these steps to set up and create your own provider:

1. **Clone the repository**:
   - Click "Use this template" and create a new repository for your provider.
   - Make sure to check **"Include all branches"** as the **build** branch is required.

2. **Project structure**:
   - The `providers` folder contains an example provider that demonstrates how to create your own. You can use this as a starting point.
   - The `src/main` folder holds the code that drives your provider.
   - **Important files**:
      - `build.gradle.kts`: Contains build configuration. Open it and replace placeholders (e.g., authors, versions, repository url) with your information. _Follow the comments_ in the file for guidance.

3. **Create your provider**:
   - Customize the example provider in the `providers` folder or create a new one by following the project structure.

     Note: When creating or modifying a provider you must edit the `settings.gradle.kts` on the root project and edit the include block.
     ```kotlin
        include(    
           "BasicDummyProviderRenamed", // <- renamed provider
           "NewProvider" // <- newly created provider
        )
     ```
   - Write the code for your provider that fetches media content from the source you want to integrate with.

4. **Build and deploy**:
   - To package your provider, run the following command in your terminal.:
     ```bash
     ./gradlew :NewProvider:make
     ```
     This will build your provider into a distributable package.

   - However, to test your provider on an online emulator or your device, use the following command:
     ```bash
     ./gradlew :NewProvider:deployWithAdb
     ``` 
     The `deployWithAdb` task depends on the `make` task so running it will automatically build and deploy the provider to the app.

   - If you are using the _debug version of the app_, add the `--debug-app` argument:
     ```bash
     ./gradlew :NewProvider:deployWithAdb --debug-app
     ```

   - If you want to attach a debugger tool, add the `--wait-for-debugger`:
     ```bash
     ./gradlew :NewProvider:deployWithAdb --wait-for-debugger
     ```
     Then go ahead and click the attach debugger button.
   
     <img src="https://i.imgur.com/d1k3ZZD.png" alt="Attach debugger to android process screenshot">

5. **Testing your provider**:
   - Once deployed, you can test how your provider behaves within the Flixclusive app. Check if it successfully fetches content from the specified sources.
   - There is also a **"Test provider"** option in the app to automate the testing.

## Additional Resources

- [**Documentation**](https://flixclusiveorg.github.io/provider-docs/)
- [**Provider API Reference**](https://flixclusiveorg.github.io/core-stubs/)

## Support

If you encounter any issues or have questions, feel free to join the discord community and ask for guidance there.

<a href="https://discord.gg/7yPSPveReu"><img src="https://img.shields.io/discord/1255770492049162240?label=discord&labelColor=7289da&color=2c2f33&style=for-the-badge" alt="Discord"></a>
