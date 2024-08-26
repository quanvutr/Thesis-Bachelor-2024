import datetime
import glob
import os

import cv2
import findspark
import numpy as np
from django.core.files.storage import FileSystemStorage
from django.http import HttpRequest, HttpResponse
from keras import Model
from pyspark.context import SparkContext
from pyspark.ml.feature import BucketedRandomProjectionLSHModel, BucketedRandomProjectionLSH
from pyspark.ml.linalg import Vectors
from pyspark.sql import SQLContext
from tensorflow import keras

from SimilarFacesProject import settings

TRAIN_VECTOR_DIR = r'D:/\Thesis-Bachelor-2024/Server/SimilarFacesRetrieval/media/FaceScrub-Dataset/Train'
CAE_CHECKPOINT_DIR = r'D:/\Thesis-Bachelor-2024/Server/SimilarFacesRetrieval/MLModels/CAE-32'
BRP_MODEL_DIR = r'D:\\Thesis-Bachelor-2024\Server\SimilarFacesRetrieval\MLModels\BRP'
CASCADE_META_FILE = r'D:/\Thesis-Bachelor-2024/Server\SimilarFacesRetrieval/MLModels/haarcascade_frontalface_default.xml'
IMAGE_SIZE = 128
DATA_SET_PREFIX = r'D:/Thesis-Bachelor-2024/Server/media/FaceScrub-Dataset'

brpModel = None
CAEModel = None
CAEEncoder = None
dfFaces = None


def initServices():
    sc, sqlc = initPySpark()
    global dfFaces
    dfFaces = loadFaceDataFrame(sc, sqlc)
    print('dfFaces loaded')
    global brpModel
    # brpModel = BucketedRandomProjectionLSHModel.load(BRP_MODEL_DIR)
    brpModel = BucketedRandomProjectionLSH(inputCol="feature",
                                           outputCol="hashes",
                                           bucketLength=64.0,
                                           numHashTables=256)
    brpModel = brpModel.fit(dfFaces)
    print('brp model loaded')
    global CAEModel
    CAEModel = keras.models.load_model(CAE_CHECKPOINT_DIR)
    print('CAE model loaded')
    global CAEEncoder
    CAEEncoder = Model(CAEModel.input, CAEModel.get_layer("pool4").output)
    print('CAE Encoder loaded')


def loadFaceDataFrame(sc, sqlc):
    global dfFaces
    featureVectorsFileNames = glob.glob(TRAIN_VECTOR_DIR + '/*/*.npy')
    featureVectorsFileNames.sort()

    # Create table with 3 fields(path, basename, feature)
    featureVectorsFileNames = sc.parallelize(featureVectorsFileNames)
    dataRows = featureVectorsFileNames \
        .map(lambda x: (x, os.path.basename(x),
                        Vectors.dense(np.load(x).reshape(-1)))
             )
    dfFaces = sqlc.createDataFrame(dataRows, schema=['absolute_path', 'basename', 'feature'])
    return dfFaces


def initPySpark():
    findspark.init()
    os.environ["JAVA_HOME"] = "C:/Program Files/Java/jdk1.8.0_231"
    os.environ["SPARK_HOME"] = "C:/spark/spark-3.4.0-bin-hadoop3"
    sc = SparkContext(master='local', appName='FaceScrub CAE')
    sqlc = SQLContext(sc)
    return sc, sqlc


def npy2jpg(path):
    return path.replace('.npy', '.jpg') \
        .replace(DATA_SET_PREFIX, 'FaceScrub-Dataset')


def toURLs(resultPaths):
    fss = FileSystemStorage()
    tmp = [npy2jpg(os.path.join(settings.MEDIA_ROOT, it)) for it in resultPaths]
    return [fss.url(it) for it in tmp]


def retrieve(img, num=10):
    img = img.astype(np.float64) / 255.
    global CAEEncoder
    assert CAEEncoder != None
    encodingVector = CAEEncoder.predict(np.array([img]))[0].reshape(-1)
    encodingVector = Vectors.dense(encodingVector)
    global brpModel
    assert brpModel != None
    global dfFaces
    assert dfFaces != None
    results = brpModel.approxNearestNeighbors(dfFaces, encodingVector, num)
    resultPaths = [it['absolute_path'] for it in results.select(['absolute_path']).collect()]
    temp = toURLs(resultPaths)
    for it in temp:
        print(it)
    return ';'.join(temp)


def extractLargestFace(image_path):
    face_cascade = cv2.CascadeClassifier(CASCADE_META_FILE)
    image = cv2.imread(image_path)
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray_image, scaleFactor=1.1, minNeighbors=5)

    largest_face = None
    largest_area = 0
    for (x, y, w, h) in faces:
        if w * h > largest_area:
            largest_area = w * h
            largest_face = (x, y, w, h)

    if largest_face is not None:
        x, y, w, h = largest_face
        face_image = image[y:y + h, x:x + w, :]
        return cv2.resize(face_image, (IMAGE_SIZE, IMAGE_SIZE))
    return None


def processImage(image_path):
    if CAEEncoder is None:
        initServices()
    selected_face = extractLargestFace(image_path)
    return [] if selected_face is None else retrieve(selected_face)


def submit(request: HttpRequest) -> HttpResponse:
    if request.method == 'POST' and request.FILES['image']:
        upload = request.FILES['image']
        fss = FileSystemStorage()
        filename = datetime.datetime.now().strftime('%Y%m%d-%H%M%S-') + upload.name
        filename = os.path.join(settings.MEDIA_ROOT, 'images/' + filename)
        file = fss.save(filename, upload)
        file_url = fss.url(file)
        resultURLs = processImage(filename)
        return HttpResponse(resultURLs)
    return HttpResponse('invalid')


def test():
    initServices()
    import sys
    image_path = sys.argv[1]
    results = processImage(image_path)
    if results != None:
        for it in results:
            print(it)


if __name__ == '__main__':
    test()
