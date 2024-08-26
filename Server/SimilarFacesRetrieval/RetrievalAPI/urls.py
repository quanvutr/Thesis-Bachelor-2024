from django.urls import path, include

from RetrievalAPI import views
from rest_framework.routers import DefaultRouter
# router = DefaultRouter()
# router.register('api/media/images', ObjectViewSet)


urlpatterns = [
    # path('', include(router.urls)),
    path('submit', views.submit, name='submit'),
]
