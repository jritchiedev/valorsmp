import React, { useEffect, useState } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useColorScheme, ActivityIndicator, View } from 'react-native';
import * as SplashScreen from 'expo-splash-screen';
import { getDatabase } from '@/src/db/database';
import { lightTheme, darkTheme } from '@/src/theme';

export { ErrorBoundary } from 'expo-router';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const theme = colorScheme === 'dark' ? darkTheme : lightTheme;
  const [ready, setReady] = useState(false);

  useEffect(() => {
    getDatabase()
      .then(() => setReady(true))
      .then(() => SplashScreen.hideAsync());
  }, []);

  if (!ready) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: theme.background }}>
        <ActivityIndicator size="large" color={theme.primary} />
      </View>
    );
  }

  return (
    <>
      <StatusBar style="auto" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: theme.background },
          headerTintColor: theme.primary,
          headerTitleStyle: { fontWeight: '600', color: theme.text },
          headerShadowVisible: false,
          contentStyle: { backgroundColor: theme.background },
          headerBackButtonDisplayMode: 'minimal',
        }}
      >
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen
          name="fluency/session"
          options={{ headerShown: false, gestureEnabled: false }}
        />
        <Stack.Screen
          name="fluency/review"
          options={{ title: 'Review' }}
        />
        <Stack.Screen
          name="fluency/results"
          options={{ title: 'Results', headerBackVisible: false, gestureEnabled: false }}
        />
        <Stack.Screen
          name="students/[id]"
          options={{ title: '' }}
        />
        <Stack.Screen
          name="passages/manage"
          options={{ title: 'Passages' }}
        />
        <Stack.Screen
          name="privacy"
          options={{ title: 'Privacy' }}
        />
      </Stack>
    </>
  );
}
