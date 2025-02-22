{
  # REF: https://github.com/tailscale/tailscale-android/blob/0ec9167cd21f18812d0bb4653bfb3459bcb0f205/flake.nix
  description = "Kodama build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    android.url = "github:tadfisher/android-nixpkgs";
    android.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = { self, nixpkgs, android }:
    let
      supportedSystems = [ "x86_64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAllSystems = f: nixpkgs.lib.genAttrs supportedSystems (system: f system);
    in
    {
      devShells = forAllSystems
        (system:
          let
            pkgs = import nixpkgs {
              inherit system;
            };
            android-sdk = android.sdk.${system} (sdkPkgs: with sdkPkgs;
              [
                build-tools-35-0-1
                cmdline-tools-latest
                platform-tools
                platforms-android-31
                platforms-android-30
                ndk-28-0-13004108
              ]);
          in
          {
            default = (with pkgs; buildFHSEnv {
              name = "bonsai";
              profile = ''
                export ANDROID_SDK_ROOT="${android-sdk}/share/android-sdk"
                export JAVA_HOME="${jdk17.home}"
              '';
              targetPkgs = pkgs: with pkgs; [
                android-sdk
                jdk17  # Android's current supported Java version
                clang
              ] ++ (if stdenv.isLinux then [
                vulkan-headers
                libxkbcommon
                wayland
                xorg.libX11
                xorg.libXcursor
                xorg.libXfixes
                libGL
                #pkgconfig
              ] else [ ]);
            }).env;
          }
        );
    };
}
