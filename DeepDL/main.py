import argparse
import pickle
import time
import math
import os
import torch
import torch.nn as nn
import data
from model import DeepDLModel
import torch.optim as optim
import datetime
from data import fetch_dataloaders
from tqdm import tqdm
import pandas as pd
import numpy as np
import csv


def read_args():

    parser = argparse.ArgumentParser(description='Source code language model')

    parser.add_argument('-project', type=str, default='activemq',
                        help='name of the dataset')
    parser.add_argument('--embed_size', type=int, default=200,
                        help='size of word embeddings')
    parser.add_argument('-hidden_size', type=int, default=32,
                        help='the number of nodes in hidden layers')
    parser.add_argument('--nhid', type=int, default=200,
                        help='number of hidden units per layer')
    parser.add_argument('--nlayers', type=int, default=2,
                        help='number of layers')
    parser.add_argument('-nhead', type=int, default=6,
                        help='the number of heads in the encoder/decoder of the transformer model')
    parser.add_argument('--lr', type=float, default=0.1,
                        help='initial learning rate')
    parser.add_argument('--clip', type=float, default=0.25,
                        help='gradient clipping')
    parser.add_argument('--epochs', type=int, default=50,
                        help='upper epoch limit')
    parser.add_argument('--batch_size', type=int, default=16, metavar='N',
                        help='batch size')
    parser.add_argument('--eval_batch_size', type=int, default=16, metavar='N',
                        help='eval batch size')
    parser.add_argument('--eval_batch_num', type=int, default=1, metavar='N',
                        help='eval batch num')
    parser.add_argument('--line_length', type=int, default=64,
                        help='added line length')
    parser.add_argument('--context_length', type=int, default=256,
                        help='context length')
    parser.add_argument('--dropout', type=float, default=0.5,
                        help='dropout applied to layers (0 = no dropout)')
    parser.add_argument('--seed', type=int, default=1111,
                        help='random seed')
    parser.add_argument('--cuda', action='store_true',
                        help='use CUDA')
    parser.add_argument('--log-interval', type=int, default=200, metavar='N',
                        help='report interval')
    parser.add_argument('--save', type=str, default='model.pt',
                        help='path to save the final model')
    parser.add_argument('--onnx-export', type=str, default='',
                        help='path to export the final model in onnx format')
    parser.add_argument('--model', type=str, default='LSTM',
                        help='type of recurrent net (RNN_TANH, RNN_RELU, LSTM, GRU, Transformer)')
    parser.add_argument('--data', type=str, default='/home/qiufangcheng/workspace/OVCNLM/data/dataset/',
                        help='location of the data corpus')
    parser.add_argument('--dry-run', action='store_true',
                        help='verify the code and the model')
    parser.add_argument('--flag', type=str, default='train',
                        help='train the model or calculate the entropy')
    parser.add_argument('-no-cuda', action='store_true', default=False,
                        help='disable the GPU')
    parser.add_argument('-save_dir', type=str, default='./snapshot',
                        help='where to save the snapshot')
    parser.add_argument('--original_test_size', type=int, default=1, metavar='N',
                        help='original test set size')


    return parser


def batchify(data, bsz, device):
    # Work out how cleanly we can divide the dataset into bsz parts.
    nbatch = data.size(0) // bsz
    # Trim off any extra elements that wouldn't cleanly fit (remainders).
    data = data.narrow(0, 0, nbatch * bsz)
    # Evenly divide the data across the bsz batches.
    data = data.view(bsz, -1).t().contiguous()
    return data.to(device)


def get_batch(params, source, i):
    seq_len = min(params.bptt, len(source) - 1 - i)
    data = source[i:i+seq_len]
    target = source[i+1:i+1+seq_len].view(-1)
    return data, target


def eachline_entropy(entropy_list, params):
    line_entropy = [entropy_list[i:i+params.bptt] for i in range(0, len(entropy_list), params.bptt)]

    avg_entropy = []
    max_entropy = []
    am_entropy = []
    for line in line_entropy:
        avg_entropy.append(sum(line) / len(line))
        max_entropy.append(max(line))
        am_entropy.append((sum(line) / len(line))+max(line))
    return am_entropy, avg_entropy, max_entropy


def context_eachline_entropy(entropy_list, corpus, params):
    context_entropy = [entropy_list[i:i + params.bptt] for i in range(0, len(entropy_list), params.bptt)]
    eol = corpus.dictionary.word_to_id['<EOL>']
    avg_entropy = []

    for i in range(0, len(corpus.test)):
        context = corpus.test[i]
        entropy = context_entropy[i]
        # line1_num = context.index(eol)
        # line2 = context[line1_num + 1:len(context)]
        # line2_num = line2.index(eol) + line1_num + 1
        # line3 = context[line2_num + 1:len(context)]
        # line3_num = line3.index(eol) + line2_num + 1
        index = np.argwhere(context == eol)
        line2_num = int(index[1])
        line3_num = int(index[2])

        target_entropy = entropy[line2_num + 1:line3_num + 1]
        avg_entropy.append(sum(target_entropy) / len(target_entropy))
    return avg_entropy


def store_result(avg_entropy, params, all_csv_path, test_csv_path):
    data = pd.read_csv(all_csv_path)
    data = np.array(data)

    data = data[0:params.original_test_size]

    test_num = len(data)


    avg_file = []
    for i in range(0, test_num):
        result_line = data[i].tolist()
        result_line.append(avg_entropy[i])
        avg_file.append(result_line)

    # print(avg_file[1])
    head = ['commit_hash', 'content', 'file_pre', 'file_new', 'line_num', 'author', 'time', 'bug_introducing', 'commit_label', 'entropy']


    with open(test_csv_path, 'w') as f:
        writer = csv.writer(f)
        writer.writerow(head)
        for line in avg_file:
            writer.writerow(line)


def train_model(params, train_data):
    params.save_dir = os.path.join(params.save_dir, datetime.datetime.now().strftime('%Y-%m-%d_%H-%M-%S'))
    model = DeepDLModel(params.vocab_size, params.line_length, params.embed_size, params.nhead, params.hidden_size,
                             params.nlayers, params.dropout).to(params.device)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.SGD(model.parameters(), lr=params.lr, momentum=0.5)


    for epoch in range(1, params.epochs + 1):
        total_loss = 0.
        start_time = time.time()

        for i, (data, target) in enumerate(tqdm(train_data)):
            # data = torch.cuda.LongTensor(data.to(params.device)).view(-1, params.batch_size)
            # target = torch.cuda.LongTensor(target.to(params.device)).view(-1)
            data = torch.cuda.LongTensor(data.to(params.device))
            target = torch.cuda.LongTensor(target.to(params.device))
            optimizer.zero_grad()

            output = model(data,target)
            # output = output.view(-1, params.vocab_size)
            loss = criterion(output, target.view(-1))
            loss.backward()
            optimizer.step()

            torch.nn.utils.clip_grad_norm_(model.parameters(), params.clip)
            total_loss += loss.item()
            if params.dry_run:
                break
        print('Training: Epoch %i / %i -- Total loss: %f' % (epoch, params.epochs, total_loss))


    save_own(model, params.save_dir, params.project)

def save_own(model, save_dir, save_prefix):
    if not os.path.isdir(save_dir):
        os.makedirs(save_dir)
    save_prefix = os.path.join(save_dir, save_prefix)
    save_path = '{}.pt'.format(save_prefix)
    torch.save(model.state_dict(), save_path)


def eval_model(params, test_data, all_csv_path, test_csv_path, corpus):
    params.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = DeepDLModel(params.vocab_size, params.line_length, params.context_length, params.embed_size, params.nhead, params.hidden_size,
                             params.nlayers, params.dropout).to(params.device)
    model.load_state_dict(torch.load(params.load_model))
    # model = torch.load('/home/qiufangcheng/workspace/OVCNLM/snapshot/2020-11-13_08-49-35/activemq.pt')
    model.eval()

    each_criterion = nn.CrossEntropyLoss(reduction='none')
    criterion = nn.CrossEntropyLoss()

    # batch_num * batch_size * bptt
    entropy_list = []
    with torch.no_grad():
        for i, (data, target) in enumerate(tqdm(test_data)):
            data = torch.cuda.LongTensor(data.to(params.device)).view(-1, params.batch_size)
            target = torch.cuda.LongTensor(target.to(params.device)).view(-1)
            output = model(data)
            output = output.view(-1, params.vocab_size)

            entropy = each_criterion(output, target).cpu().numpy().tolist()
            entropy_list.extend(entropy)

    am_entropy, avg_entropy, max_entropy = eachline_entropy(entropy_list, params)
    store_result(avg_entropy, params, all_csv_path, test_csv_path)


def main():
    params = read_args().parse_args()
    os.environ["CUDA_VISIBLE_DEVICES"] = '1'
    params.cuda = (not params.no_cuda) and torch.cuda.is_available()
    del params.no_cuda
    params.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

    params.train = True
    params.predict = False

    root = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/'
    projects = ["activemq"]

    for project in projects:
        params.project = project
        train_path = project + '/' + project + '_project_train.java'
        test_path = project + '/' + project + '_project_test.java'
        all_csv_path = root + project + '/' + project + '_add.csv'
        # test_csv_path = root + project + '/' + project + '_project_avg.csv'

        # 测试
        test_csv_path = root + project + '/' + project + '_project_avg_og.csv'

        corpus = data.Corpus(params.data, train_path, test_path, params.line_length+1)

        # print(corpus.train.shape)
        # train_data = batchify(corpus.train, params.batch_size, params.device)
        # test_data = batchify(corpus.test, params.eval_batch_size, params.device)
        params.vocab_size = len(corpus.dictionary)
        params.original_test_size = len(corpus.test)

        train_loader, test_loader = fetch_dataloaders(params, corpus)
        # if params.train is True:
        #
        #     train_model(params, train_loader)
        #
        # if params.predict is True:
        #
        #     params.load_model = '/home/qiufangcheng/workspace/OVCNLM/snapshot/2020-12-29_09-31-35/h2o.pt'
        #     eval_model(params, test_loader, all_csv_path, test_csv_path, corpus)


if __name__ == '__main__':
    main()

