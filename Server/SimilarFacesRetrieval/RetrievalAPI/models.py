from django.db import models


def upload_to(instance, filename):
    return 'images/{filename}'.format(filename=filename)


class Submission(models.Model):
    tittle = models.CharField(max_length=250)
    perImg = models.ImageField(upload_to=upload_to, blank=True, null=True)
