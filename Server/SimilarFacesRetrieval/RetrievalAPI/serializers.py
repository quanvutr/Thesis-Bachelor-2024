from rest_framework import serializers
from .models import Submission


class ObjectSerializer(serializers.ModelSerializer):
    class Meta:
        model = Submission
        fields = '__all__'
