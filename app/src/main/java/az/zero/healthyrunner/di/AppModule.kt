package az.zero.healthyrunner.di

import android.content.Context
import androidx.room.Room
import az.zero.healthyrunner.db.RunDao
import az.zero.healthyrunner.db.RunningDatabase
import az.zero.healthyrunner.utils.Constants.RUNNING_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRunningDatabase(@ApplicationContext app: Context): RunningDatabase =
        Room.databaseBuilder(app, RunningDatabase::class.java, RUNNING_DATABASE_NAME).build()

    @Provides
    @Singleton
    fun provideRunDao(db: RunningDatabase): RunDao = db.getRunDao()


}